import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ApiError } from '../api/client'
import type { OrderDto } from '../api/orders'
import { getUserOrders } from '../api/orders'
import { getProductById } from '../api/products'
import type { UserDto } from '../api/user'
import { getCurrentUser } from '../api/user'
import { useAuth } from '../auth/AuthContext'
import { KITCHEN_BOARD_PERMISSION, hasPermission, staffRoleTitle } from '../auth/permissions'
import { Icon, OrderProgress, SkeletonRows } from '../components'
import { ProductPhoto } from '../components/ProductPhoto'
import { formatMoney } from '../format'
import { useRepeatOrder } from '../menu/useRepeatOrder'
import styles from './Cream.module.css'

/** Orders the customer is still waiting for (or can pick up right now). */
const ACTIVE_STATUSES = new Set(['NEW', 'IN_PROGRESS', 'READY'])
const ACTIVE_POLL_INTERVAL_MS = 15_000

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; user: UserDto; orders: OrderDto[]; productImages: Map<number, string | null> }

function formatOrderDate(value: string) {
  return new Date(value).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' })
}

function initials(user: UserDto) {
  const source = user.name?.trim() || user.email
  const parts = source.split(/\s+/).filter(Boolean)
  return (parts.length > 1 ? `${parts[0]![0]}${parts[1]![0]}` : source.slice(0, 2)).toUpperCase()
}

export function ProfileScreen() {
  const { logout, session } = useAuth()
  const isStaff = hasPermission(session?.role, KITCHEN_BOARD_PERMISSION, session?.permissions)
  const navigate = useNavigate()
  const { repeat, repeatingOrderId, repeatError } = useRepeatOrder()
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [confirmLogout, setConfirmLogout] = useState(false)

  useEffect(() => {
    let cancelled = false

    Promise.all([getCurrentUser(), getUserOrders()])
      .then(async ([user, orders]) => {
        if (cancelled) return

        const productIds = Array.from(new Set(orders.flatMap((order) => order.items.map((item) => item.productId))))
        const productImages = new Map<number, string | null>()
        await Promise.all(
          productIds.map(async (productId) => {
            try {
              const product = await getProductById(productId)
              productImages.set(productId, product.imagePath)
            } catch {
              productImages.set(productId, null)
            }
          }),
        )

        if (!cancelled) setState({ status: 'ready', user, orders, productImages })
      })
      .catch((err) => {
        if (!cancelled) {
          setState({ status: 'error', message: err instanceof ApiError ? err.message : 'Не удалось загрузить профиль.' })
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const hasActiveOrders = state.status === 'ready' && state.orders.some((order) => ACTIVE_STATUSES.has(order.status))

  // While something is still being prepared, keep the statuses fresh: poll, and refresh
  // at once when a push ("order is ready") arrives.
  useEffect(() => {
    if (!hasActiveOrders) return
    let cancelled = false

    function refresh() {
      getUserOrders()
        .then((orders) => {
          if (!cancelled) setState((current) => (current.status === 'ready' ? { ...current, orders } : current))
        })
        .catch(() => {
          // Keep showing the last known statuses; the next tick retries.
        })
    }

    function handlePushMessage(event: MessageEvent) {
      if ((event.data as { type?: string } | null)?.type === 'push') refresh()
    }

    const intervalId = window.setInterval(refresh, ACTIVE_POLL_INTERVAL_MS)
    navigator.serviceWorker?.addEventListener('message', handlePushMessage)

    return () => {
      cancelled = true
      window.clearInterval(intervalId)
      navigator.serviceWorker?.removeEventListener('message', handlePushMessage)
    }
  }, [hasActiveOrders])

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <section className={styles.screen} aria-labelledby="profile-title">
      <h1 className={styles.pageTitle} id="profile-title">
        Профиль
      </h1>

      {state.status === 'loading' ? <SkeletonRows label="Загружаем профиль…" count={4} trailing={false} /> : null}
      {state.status === 'error' ? (
        <p className={styles.alert} role="alert">
          <Icon name="alert" size={16} />
          {state.message}
        </p>
      ) : null}

      {state.status === 'ready' ? (
        <>
          <Link className={styles.card} to="/profile/edit">
            <span className={styles.avatar} aria-hidden="true">
              {initials(state.user)}
            </span>
            <span className={styles.cardBody}>
              <span className={styles.cardTitle}>{state.user.name ?? state.user.email}</span>
              <span className={styles.cardMeta}>{state.user.phone ?? state.user.email}</span>
            </span>
            <Icon name="edit" size={18} className={styles.chevron} />
          </Link>

          {isStaff ? (
            <Link className={`${styles.card} ${styles.cardAccent}`} to="/staff">
              <span className={styles.cardIcon}>
                <Icon name="briefcase" />
              </span>
              <span className={styles.cardBody}>
                <span className={styles.cardTitle}>Рабочее место</span>
                <span className={styles.cardMeta}>{staffRoleTitle(session?.role)}: заказы, касса, меню</span>
              </span>
              <Icon name="right" size={18} className={styles.chevron} />
            </Link>
          ) : null}

          {hasActiveOrders ? (
            <section className={styles.section} aria-labelledby="active-orders-title">
              <div className={styles.sectionHead}>
                <h2 className={styles.sectionTitle} id="active-orders-title">
                  Текущие заказы
                </h2>
              </div>
              {state.orders
                .filter((order) => ACTIVE_STATUSES.has(order.status))
                .map((order) => (
                  <Link className={styles.activeOrder} key={order.id} to={`/order/${order.id}`}>
                    <span className={styles.activeOrderHead}>
                      <span>Заказ №{order.dailyNumber ?? order.id}</span>
                      <span>{formatMoney(order.total)}</span>
                    </span>
                    <span className={styles.cardMeta}>
                      {order.shopName} · {order.items.map((item) => item.productName).join(', ')}
                    </span>
                    <OrderProgress status={order.status} />
                  </Link>
                ))}
            </section>
          ) : null}

          <section className={styles.section} aria-labelledby="orders-title">
            <div className={styles.sectionHead}>
              <h2 className={styles.sectionTitle} id="orders-title">
                История заказов
              </h2>
            </div>

            {state.orders.length === 0 ? (
              <p className={styles.muted}>Заказов пока нет — самое время попробовать что-нибудь новое.</p>
            ) : state.orders.every((order) => ACTIVE_STATUSES.has(order.status)) ? (
              <p className={styles.muted}>Завершённых заказов пока нет.</p>
            ) : (
              <div className={styles.history}>
                {state.orders
                  .filter((order) => !ACTIVE_STATUSES.has(order.status))
                  .map((order) => (
                    <div className={styles.historyRow} key={order.id}>
                      <Link className={styles.historyLink} to={`/order/${order.id}`}>
                        <span className={styles.thumbs} aria-hidden="true">
                          {order.items.slice(0, 3).map((item) => (
                            <ProductPhoto
                              key={item.id}
                              className={styles.photo}
                              imagePath={state.productImages.get(item.productId)}
                              width={44}
                            />
                          ))}
                        </span>
                        <span className={styles.cardBody}>
                          <span className={styles.cardTitle}>{formatMoney(order.total)}</span>
                          <span className={styles.cardMeta}>
                            {formatOrderDate(order.createdAt)} · {order.shopName} · {order.statusNameRu}
                          </span>
                        </span>
                      </Link>
                      {order.status === 'COMPLETED' ? (
                        <button
                          className={`${styles.btn} ${styles.btnSecondary}`}
                          disabled={repeatingOrderId === order.id}
                          onClick={() => void repeat(order)}
                          type="button"
                        >
                          <Icon name="refresh" size={16} />
                          Повторить
                        </button>
                      ) : null}
                    </div>
                  ))}
              </div>
            )}

            {repeatError ? <p className={styles.muted}>{repeatError}</p> : null}
          </section>
        </>
      ) : null}

      {confirmLogout ? (
        <div className={styles.confirm} role="alertdialog" aria-labelledby="logout-question">
          <p id="logout-question">Выйти из аккаунта?</p>
          <div className={styles.confirmActions}>
            <button className={`${styles.btn} ${styles.btnDanger}`} onClick={handleLogout} type="button">
              Выйти
            </button>
            <button className={`${styles.btn} ${styles.btnGhost}`} onClick={() => setConfirmLogout(false)} type="button">
              Остаться
            </button>
          </div>
        </div>
      ) : (
        <button className={styles.logout} onClick={() => setConfirmLogout(true)} type="button">
          <Icon name="logout" size={18} />
          Выйти из аккаунта
        </button>
      )}
    </section>
  )
}
