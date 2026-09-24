import { classNames } from '../classNames'
import styles from './OrderProgress.module.css'

const STEPS = [
  { code: 'NEW', label: 'Принят' },
  { code: 'IN_PROGRESS', label: 'Готовится' },
  { code: 'READY', label: 'Готов' },
] as const

export type OrderProgressProps = {
  /** Order status code: NEW, IN_PROGRESS, READY (COMPLETED shows every step done). */
  status: string
  className?: string | undefined
}

/** Three-step status bar: which stage an active order is at, at a glance. */
export function OrderProgress({ status, className }: OrderProgressProps) {
  const currentIndex = status === 'COMPLETED' ? STEPS.length - 1 : STEPS.findIndex((step) => step.code === status)
  const isReady = status === 'READY' || status === 'COMPLETED'

  return (
    <ol
      className={classNames(styles.progress, isReady ? styles.ready : undefined, className)}
      aria-label={currentIndex >= 0 ? `Статус заказа: ${STEPS[currentIndex]!.label}` : 'Статус заказа'}
    >
      {STEPS.map((step, index) => (
        <li
          className={classNames(
            styles.step,
            index <= currentIndex ? styles.done : undefined,
            index === currentIndex ? styles.current : undefined,
          )}
          key={step.code}
          aria-current={index === currentIndex ? 'step' : undefined}
        >
          <span className={styles.bar} aria-hidden="true" />
          <span className={styles.label}>{step.label}</span>
        </li>
      ))}
    </ol>
  )
}
