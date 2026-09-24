import { useEffect, useState } from 'react'

import { enablePush, getPushConfig, getPushSupport, syncExistingSubscription } from './push'
import styles from './ReadyNotificationCard.module.css'

type CardState =
  | { status: 'loading' }
  | { status: 'hidden' }
  | { status: 'needs-install' }
  | { status: 'available'; publicKey: string }
  | { status: 'enabling'; publicKey: string }
  | { status: 'enabled' }
  | { status: 'denied' }
  | { status: 'error'; publicKey: string }

/** Offers a push notification for when the customer's order becomes ready for pickup. */
export function ReadyNotificationCard() {
  const [state, setState] = useState<CardState>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    const support = getPushSupport()

    async function load(): Promise<CardState> {
      if (support === 'unsupported') return { status: 'hidden' }
      const config = await getPushConfig()
      if (!config.enabled) return { status: 'hidden' }
      if (support === 'needs-install') return { status: 'needs-install' }
      if (Notification.permission === 'denied') return { status: 'denied' }
      if (await syncExistingSubscription()) return { status: 'enabled' }
      return { status: 'available', publicKey: config.publicKey }
    }

    load()
      .then((next) => {
        if (!cancelled) setState(next)
      })
      .catch(() => {
        if (!cancelled) setState({ status: 'hidden' })
      })

    return () => {
      cancelled = true
    }
  }, [])

  async function handleEnable(publicKey: string) {
    setState({ status: 'enabling', publicKey })
    try {
      const result = await enablePush(publicKey)
      setState(result === 'enabled' ? { status: 'enabled' } : { status: 'denied' })
    } catch {
      setState({ status: 'error', publicKey })
    }
  }

  if (state.status === 'loading' || state.status === 'hidden') return null

  return (
    <section className={styles.card} aria-live="polite">
      <span className={styles.icon} aria-hidden="true">
        🔔
      </span>
      <div className={styles.body}>
        {state.status === 'enabled' ? (
          <>
            <p className={styles.title}>Уведомления включены</p>
            <p className={styles.text}>Пришлём уведомление, когда заказ будет готов.</p>
          </>
        ) : state.status === 'denied' ? (
          <>
            <p className={styles.title}>Уведомления запрещены</p>
            <p className={styles.text}>Разрешите уведомления для этого сайта в настройках браузера.</p>
          </>
        ) : state.status === 'needs-install' ? (
          <>
            <p className={styles.title}>Уведомления на iPhone</p>
            <p className={styles.text}>
              Добавьте сайт на экран «Домой» (Поделиться → На экран «Домой»), откройте его оттуда и включите
              уведомления.
            </p>
          </>
        ) : (
          <>
            <p className={styles.title}>Сообщить, когда будет готов?</p>
            <p className={styles.text}>
              {state.status === 'error'
                ? 'Не удалось включить уведомления. Попробуйте ещё раз.'
                : 'Пришлём уведомление, даже если закроете страницу.'}
            </p>
            <button
              className={styles.button}
              type="button"
              disabled={state.status === 'enabling'}
              onClick={() => void handleEnable(state.publicKey)}
            >
              {state.status === 'enabling' ? 'Включаем…' : 'Включить уведомления'}
            </button>
          </>
        )}
      </div>
    </section>
  )
}
