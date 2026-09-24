import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { ApiError } from '../api/client'
import type { OrderDto } from '../api/orders'
import { cancelOrder, getOrderById } from '../api/orders'
import type { BadgeTone } from '../components'
import { Badge } from '../components'
import styles from './Screens.module.css'

const POLL_INTERVAL_MS = 30_000
const TERMINAL_STATUSES = new Set(['COMPLETED', 'CANCELLED'])

type LoadState = { status: 'loading' } | { status: 'error'; message: string } | { status: 'ready'; order: OrderDto }

function formatMoney(amount: number) {
  return `${amount.toLocaleString('ru-RU')} ₸`
}

function formatTime(value: string) {
  const date = new Date(value)
  return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
}

function statusTone(statusCode: string): BadgeTone {
  switch (statusCode) {
    case 'READY':
    case 'COMPLETED':
      return 'green'
    case 'IN_PROGRESS':
      return 'orange'
    case 'CANCELLED':
      return 'muted'
    default:
      return 'blue'
  }
}

export function OrderStatusScreen() {
  const { orderId } = useParams()

  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [isCancelling, setIsCancelling] = useState(false)
  const [lastCheckedAt, setLastCheckedAt] = useState<Date | null>(null)

  useEffect(() => {
    if (!orderId) return
    const id = Number(orderId)
    let cancelled = false

    async function poll() {
      try {
        const order = await getOrderById(id)
        if (cancelled) return
        setState({ status: 'ready', order })
        setLastCheckedAt(new Date())

        if (TERMINAL_STATUSES.has(order.status)) {
          window.clearInterval(intervalId)
        }
      } catch (err) {
        if (!cancelled) {
          setState({
            status: 'error',
            message: err instanceof ApiError ? err.message : 'Не удалось загрузить заказ.',
          })
        }
      }
    }

    void poll()
    const intervalId = window.setInterval(poll, POLL_INTERVAL_MS)

    return () => {
      cancelled = true
      window.clearInterval(intervalId)
    }
  }, [orderId])

  async function handleCancel() {
    if (state.status !== 'ready') return

    setIsCancelling(true)
    try {
      const updated = await cancelOrder(state.order.id)
      setState({ status: 'ready', order: updated })
    } catch (err) {
      setState({
        status: 'error',
        message: err instanceof ApiError ? err.message : 'Не удалось отменить заказ.',
      })
    } finally {
      setIsCancelling(false)
    }
  }

  return (
    <section className={styles.screen} aria-labelledby="order-status-title">
      <header className={styles.profileHeader}>
        <Link className={styles.roundIconButton} to="/profile" aria-label="Назад">
          <span aria-hidden="true">‹</span>
        </Link>
        <h1 className={styles.title} id="order-status-title">
          Заказ
        </h1>
        <span className={styles.roundIconButton} aria-hidden="true" />
      </header>

      {state.status === 'loading' ? <p className={styles.muted}>Загружаем заказ…</p> : null}
      {state.status === 'error' ? <p className={styles.muted}>{state.message}</p> : null}

      {state.status === 'ready' ? (
        <>
          <article className={styles.orderSummaryCard}>
            <div className={styles.sectionHeader}>
              <p className={styles.eyebrow}>{state.order.shopName}</p>
              <Badge tone={statusTone(state.order.status)}>{state.order.statusNameRu}</Badge>
            </div>
            <p className={styles.price}>{formatMoney(state.order.total)}</p>
            <p className={styles.muted}>Заказ №{state.order.id}</p>
            {lastCheckedAt ? (
              <p className={styles.muted}>Статус обновлён в {formatTime(lastCheckedAt.toISOString())}</p>
            ) : null}
          </article>

          <div className={styles.list}>
            {state.order.items.map((item) => (
              <article className={styles.orderCard} key={item.id}>
                <div className={styles.orderInfo}>
                  <p className={styles.cardTitle}>{item.productName}</p>
                  <p className={styles.orderMeta}>
                    {item.quantity} × {formatMoney(item.price / item.quantity)}
                    {item.toppings.length > 0 ? ` · ${item.toppings.map((t) => t.name).join(', ')}` : ''}
                  </p>
                </div>
                <span className={styles.price}>{formatMoney(item.price)}</span>
              </article>
            ))}
          </div>

          {!TERMINAL_STATUSES.has(state.order.status) ? (
            <button
              className={styles.ghostButton}
              disabled={isCancelling || state.order.status !== 'NEW'}
              onClick={handleCancel}
              type="button"
            >
              {isCancelling ? 'Отменяем…' : 'Отменить заказ'}
            </button>
          ) : null}

          <Link className={styles.linkButton} to="/profile">
            К истории заказов
          </Link>
        </>
      ) : null}
    </section>
  )
}
