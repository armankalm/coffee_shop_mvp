import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ApiError, resolveAssetUrl } from '../api/client'
import type { OrderDto } from '../api/orders'
import { getUserOrders } from '../api/orders'
import { getProductById } from '../api/products'
import type { UserDto } from '../api/user'
import { getCurrentUser } from '../api/user'
import { useAuth } from '../auth/AuthContext'
import { useCart } from '../cart/CartContext'
import { useShop } from '../shop/ShopContext'
import heroFallback from '../assets/hero.png'
import loginStyles from './LoginScreen.module.css'
import styles from './Screens.module.css'

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; user: UserDto; orders: OrderDto[]; productImages: Map<number, string | null> }

function formatMoney(amount: number) {
  return `${amount.toLocaleString('ru-RU')} ₸`
}

function formatOrderDate(value: string) {
  const date = new Date(value)
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${day}.${month}.${date.getFullYear()}`
}

function initialsFromEmail(email: string) {
  return email.slice(0, 2).toUpperCase()
}

export function ProfileScreen() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const { addItem } = useCart()
  const { shop } = useShop()
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [repeatingOrderId, setRepeatingOrderId] = useState<number | null>(null)
  const [repeatError, setRepeatError] = useState<string | null>(null)

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
          setState({
            status: 'error',
            message: err instanceof ApiError ? err.message : 'Не удалось загрузить профиль.',
          })
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  async function handleRepeatOrder(order: OrderDto) {
    setRepeatError(null)
    setRepeatingOrderId(order.id)

    if (!shop || shop.id !== order.shopId) {
      setRepeatError('Select the same coffee shop before repeating this order.')
      setRepeatingOrderId(null)
      return
    }

    try {
      const results = await Promise.all(
        order.items.map(async (item) => {
          try {
            const product = await getProductById(item.productId)
            return { item, product }
          } catch {
            return { item, product: null }
          }
        }),
      )

      const unavailableCount = results.filter(({ product }) => !product || !product.available).length

      for (const { item, product } of results) {
        if (!product || !product.available) continue
        addItem(
          product,
          item.toppings.map((topping) => topping.id),
          item.quantity,
          shop.id,
        )
      }

      if (unavailableCount > 0) {
        setRepeatError(
          unavailableCount === order.items.length
            ? 'Все товары из этого заказа сейчас недоступны.'
            : 'Некоторые товары из заказа сейчас недоступны и не были добавлены.',
        )
        return
      }

      navigate('/cart')
    } finally {
      setRepeatingOrderId(null)
    }
  }

  return (
    <section className={`${styles.screen} ${styles.profileScreen}`} aria-labelledby="profile-title">
      <header className={styles.profileHeader}>
        <Link className={styles.roundIconButton} to="/locations" aria-label="Назад">
          <span aria-hidden="true">‹</span>
        </Link>
        <h1 className={styles.title} id="profile-title">
          Профиль
        </h1>
        <button className={styles.roundIconButton} type="button" aria-label="Открыть чат">
          <span aria-hidden="true">?</span>
        </button>
      </header>

      {state.status === 'loading' ? <p className={styles.muted}>Загружаем профиль…</p> : null}
      {state.status === 'error' ? <p className={styles.muted}>{state.message}</p> : null}

      {state.status === 'ready' ? (
        <>
          <Link className={styles.profileCard} to="/profile/edit">
            <span className={styles.avatar}>{initialsFromEmail(state.user.email)}</span>
            <div className={styles.profileDetails}>
              <p className={styles.profileName}>{state.user.name ?? state.user.email}</p>
              <p className={styles.muted}>{state.user.phone ?? state.user.coffeeShopName ?? state.user.email}</p>
            </div>
            <span className={styles.profileArrow} aria-hidden="true">
              &gt;
            </span>
          </Link>

          <section className={styles.section} aria-labelledby="orders-title">
            <h2 className={styles.sectionTitle} id="orders-title">
              История заказов
            </h2>

            {state.orders.length === 0 ? (
              <p className={styles.muted}>Заказов пока нет.</p>
            ) : (
              <div className={styles.list}>
                {state.orders.map((order) => (
                  <article className={styles.orderCard} key={order.id}>
                    <Link className={styles.orderInfo} to={`/order/${order.id}`}>
                      <p className={styles.price}>{formatMoney(order.total)}</p>
                      <p className={styles.orderMeta}>
                        {formatOrderDate(order.createdAt)} · {order.shopName} · {order.statusNameRu}
                      </p>
                      <div className={styles.orderThumbs} aria-label="Напитки в заказе">
                        {order.items.slice(0, 3).map((item) => (
                          <img
                            alt=""
                            className={styles.orderThumb}
                            key={item.id}
                            src={
                              resolveAssetUrl(state.productImages.get(item.productId) ?? undefined) ?? heroFallback
                            }
                          />
                        ))}
                      </div>
                    </Link>
                    <button
                      aria-label="Повторить заказ"
                      className={styles.repeatButton}
                      disabled={repeatingOrderId === order.id}
                      onClick={() => handleRepeatOrder(order)}
                      type="button"
                    >
                      <span aria-hidden="true">↻</span>
                    </button>
                  </article>
                ))}
              </div>
            )}

            {repeatError ? <p className={styles.muted}>{repeatError}</p> : null}
          </section>
        </>
      ) : null}

      <button className={loginStyles.linkAction} onClick={handleLogout} type="button">
        Выйти из аккаунта
      </button>
    </section>
  )
}
