import { displayOrderNumber } from '../../kitchen/orderKey'
import type { OrderPosition, OrderPositionStatus } from '../../types'
import { classNames } from '../classNames'
import { Icon } from '../Icon'
import { formatElapsedTime, formatTimer, slaLevel, useElapsedMs } from './elapsedTime'
import styles from './PositionCard.module.css'

/** The action is labelled with the next step (audit B1), not «tap the card». */
const ACTIONS: Partial<Record<OrderPositionStatus, { label: string; icon: 'play' | 'check' | 'hand' }>> = {
  NEW: { label: 'Начать', icon: 'play' },
  IN_PROGRESS: { label: 'Готово', icon: 'check' },
  READY: { label: 'Выдать', icon: 'hand' },
}

export type PositionCardProps = {
  position: OrderPosition
  /** Other positions of the same order, shown as «В заказе ещё: …» (audit B4). */
  siblings?: OrderPosition[]
  onPositionClick: (id: string) => void
  disabled?: boolean
  className?: string
}

export function PositionCard({ position, siblings = [], onPositionClick, disabled, className }: PositionCardProps) {
  const elapsedMs = useElapsedMs(position.createdAt)
  const sla = slaLevel(elapsedMs)
  const action = ACTIONS[position.status]
  const elapsedLabel = formatElapsedTime(position.createdAt, Date.parse(position.createdAt) + elapsedMs)
  const orderNumber = displayOrderNumber(position.orderNumber)

  return (
    <article
      className={classNames(styles.card, className)}
      data-position-id={position.id}
      data-status={position.status}
      data-sla={sla}
      aria-busy={disabled || undefined}
      aria-label={`${orderNumber}, ${position.title}`}
    >
      <div className={styles.top}>
        <span className={styles.orderNumber}>{orderNumber}</span>
        <span className={styles.timer} aria-label={`Прошло ${elapsedLabel} с создания`}>
          <Icon name={sla === 'late' ? 'alertTriangle' : 'clock'} size={16} />
          {formatTimer(elapsedMs)}
        </span>
      </div>

      <h3 className={styles.title}>{position.title}</h3>

      {position.comment ? (
        <p className={styles.comment}>
          <Icon name="message" size={16} />
          <span>{position.comment}</span>
        </p>
      ) : null}

      {siblings.length > 0 ? (
        <p className={styles.siblings}>
          <Icon name="link" size={14} />
          <span>
            В заказе ещё:{' '}
            {siblings.map((sibling, index) => (
              <span key={sibling.id}>
                {index > 0 ? ', ' : null}
                <b>{sibling.title}</b>
                {sibling.status === 'READY' ? ' (готов)' : null}
              </span>
            ))}
          </span>
        </p>
      ) : null}

      {action ? (
        <button
          className={styles.action}
          type="button"
          disabled={disabled}
          onClick={() => onPositionClick(position.id)}
          aria-label={`${action.label}: ${orderNumber} ${position.title}`}
        >
          <Icon name={action.icon} size={20} />
          {action.label}
        </button>
      ) : null}
    </article>
  )
}
