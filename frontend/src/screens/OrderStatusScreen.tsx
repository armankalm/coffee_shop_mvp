import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'

import { ApiError } from '../api/client'
import type { OrderDto } from '../api/orders'
import { cancelOrder, getOrderById } from '../api/orders'
import { Icon, Skeleton, SkeletonStatus } from '../components'
import { formatMoney, formatTime } from '../format'
import { ReadyNotificationCard } from '../push/ReadyNotificationCard'
import { useToast } from '../toast/ToastContext'
import styles from './Cream.module.css'

/** Faster than before (audit S5) while the guest is actually waiting; pushes refresh instantly. */
const POLL_INTERVAL_MS = 15_000
const TERMINAL_STATUSES = new Set(['COMPLETED', 'CANCELLED'])

const STEPS = [
  { code: 'NEW', title: 'Принят', note: 'Заказ получен кофейней' },
  { code: 'IN_PROGRESS', title: 'Готовится', note: 'Бариста уже работает' },
  { code: 'READY', title: 'Готов', note: 'Ждёт на стойке выдачи' },
] as const

type LoadState = { status: 'loading' } | { status: 'error'; message: string } | { status: 'ready'; order: OrderDto }

function stepIndex(status: string) {
  if (status === 'COMPLETED') return STEPS.length
  return STEPS.findIndex((step) => step.code === status)
}

function StatusPill({ order }: { order: OrderDto }) {
  if (order.status === 'READY') {
    return (
      <span className={`${styles.pill} ${styles.pillSuccess}`}>
        <Icon name="check" size={16} />
        Готов — можно забирать
      </span>
    )
  }
  if (order.status === 'IN_PROGRESS') {
    return (
      <span className={`${styles.pill} ${styles.pillWarning}`}>
        <Icon name="clock" size={16} />
        Готовится
      </span>
    )
  }
  return <span className={styles.pill}>{order.statusNameRu}</span>
}

export function OrderStatusScreen() {
  const { orderId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { showToast } = useToast()

  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [isCancelling, setIsCancelling] = useState(false)
  const [confirmCancel, setConfirmCancel] = useState(false)

  useEffect(() => {
    if (!orderId) return
    const id = Number(orderId)
    let cancelled = false

    async function poll() {
      try {
        const order = await getOrderById(id)
        if (cancelled) return
        setState({ status: 'ready', order })
        if (TERMINAL_STATUSES.has(order.status)) window.clearInterval(intervalId)
      } catch (err) {
        // Keep the last known order on screen; only an initial failure replaces it (audit S4).
        if (!cancelled) {
          setState((current) =>
            current.status === 'ready'
              ? current
              : { status: 'error', message: err instanceof ApiError ? err.message : 'Не удалось загрузить заказ.' },
          )
        }
      }
    }

    void poll()
    const intervalId = window.setInterval(poll, POLL_INTERVAL_MS)

    // A push for this order ("ready") refreshes the status immediately.
    function handlePushMessage(event: MessageEvent) {
      const message = event.data as { type?: string; url?: string } | null
      if (message?.type === 'push' && message.url === `/order/${id}`) void poll()
    }
    navigator.serviceWorker?.addEventListener('message', handlePushMessage)

    return () => {
      cancelled = true
      window.clearInterval(intervalId)
      navigator.serviceWorker?.removeEventListener('message', handlePushMessage)
    }
  }, [orderId])

  async function handleCancel() {
    if (state.status !== 'ready') return
    setIsCancelling(true)
    try {
      const updated = await cancelOrder(state.order.id)
      setState({ status: 'ready', order: updated })
      setConfirmCancel(false)
      showToast({ message: 'Заказ отменён' })
    } catch (err) {
      showToast({ message: err instanceof ApiError ? err.message : 'Не удалось отменить заказ.' })
    } finally {
      setIsCancelling(false)
    }
  }

  function goBack() {
    if (location.key !== 'default') navigate(-1)
    else navigate('/home')
  }

  const order = state.status === 'ready' ? state.order : null
  const current = order ? stepIndex(order.status) : -1

  return (
    <section className={styles.screen} aria-labelledby="order-status-title">
      <div className={styles.topBar}>
        <button className={styles.iconButton} type="button" aria-label="Назад" onClick={goBack}>
          <Icon name="left" />
        </button>
        <h1 className={styles.topBarTitle} id="order-status-title">
          {order ? `Заказ от ${formatTime(order.createdAt)}` : 'Заказ'}
        </h1>
        <span aria-hidden="true" />
      </div>

      {state.status === 'loading' ? (
        <SkeletonStatus label="Загружаем заказ…">
          <Skeleton height={220} radius={24} />
          <Skeleton height={48} radius={16} />
          <Skeleton height={48} radius={16} />
          <Skeleton height={48} radius={16} />
        </SkeletonStatus>
      ) : null}

      {state.status === 'error' ? (
        <p className={styles.alert} role="alert">
          <Icon name="alert" size={16} />
          {state.message}
        </p>
      ) : null}

      {order ? (
        <>
          <div className={styles.statusCard}>
            <p className={styles.groupLabel}>{order.status === 'CANCELLED' ? 'Заказ' : 'Назовите на стойке'}</p>
            <div className={styles.bigNumber} aria-label={`Номер заказа ${order.dailyNumber ?? order.id}`}>
              {order.dailyNumber ?? order.id}
            </div>
            <StatusPill order={order} />
          </div>

          {order.status !== 'CANCELLED' ? (
            <ol className={styles.timeline} aria-label="Ход заказа">
              {STEPS.map((step, index) => {
                // «Готовится» stays an open ring while the barista works; other reached steps are ticked.
                const done =
                  index < current || (index === current && step.code !== 'IN_PROGRESS') || order.status === 'COMPLETED'
                const isCurrent = index === current && !done
                return (
                  <li
                    key={step.code}
                    data-done={done}
                    data-current={isCurrent}
                    aria-current={index === current ? 'step' : undefined}
                  >
                    <span className={styles.dot}>{done ? <Icon name="check" size={16} /> : null}</span>
                    <span>
                      {step.title}
                      <small>{step.note}</small>
                    </span>
                    <time>{index === 0 ? formatTime(order.createdAt) : ''}</time>
                  </li>
                )
              })}
            </ol>
          ) : null}

          {order.status === 'NEW' || order.status === 'IN_PROGRESS' ? <ReadyNotificationCard /> : null}

          <div className={styles.where}>
            <Icon name="pin" />
            <span>
              <b>{order.shopName}</b>
              <br />
              <span className={styles.muted}>Самовывоз · стойка выдачи</span>
            </span>
          </div>

          <div className={styles.orderItems}>
            {order.items.map((item) => (
              <div className={styles.orderItem} key={item.id}>
                <span>
                  {item.quantity > 1 ? `${item.quantity} × ` : ''}
                  {item.productName}
                  {item.toppings.length > 0 ? <small>{item.toppings.map((t) => t.name).join(', ')}</small> : null}
                </span>
                <b>{formatMoney(item.price)}</b>
              </div>
            ))}
            <div className={styles.orderItem}>
              <span>Итого</span>
              <b>{formatMoney(order.total)}</b>
            </div>
          </div>

          {order.status === 'NEW' ? (
            confirmCancel ? (
              <div className={styles.confirm} role="alertdialog" aria-labelledby="cancel-question">
                <p id="cancel-question">Отменить заказ №{order.dailyNumber ?? order.id}?</p>
                <div className={styles.confirmActions}>
                  <button
                    className={`${styles.btn} ${styles.btnDanger}`}
                    type="button"
                    onClick={handleCancel}
                    disabled={isCancelling}
                  >
                    {isCancelling ? 'Отменяем…' : 'Да, отменить'}
                  </button>
                  <button className={`${styles.btn} ${styles.btnGhost}`} type="button" onClick={() => setConfirmCancel(false)}>
                    Нет
                  </button>
                </div>
              </div>
            ) : (
              <button className={`${styles.btn} ${styles.btnGhost}`} type="button" onClick={() => setConfirmCancel(true)}>
                Отменить заказ
              </button>
            )
          ) : order.status === 'IN_PROGRESS' ? (
            <p className={styles.muted}>Заказ уже готовится — отмена недоступна.</p>
          ) : null}

          <Link className={styles.textLink} to="/profile">
            Все заказы
          </Link>
        </>
      ) : null}
    </section>
  )
}
