import type { ButtonHTMLAttributes } from 'react'

import type { OrderPosition } from '../../types'
import { classNames } from '../classNames'
import { useElapsedTime } from './elapsedTime'
import styles from './PositionCard.module.css'

export type PositionCardProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'onClick'> & {
  position: OrderPosition
  onPositionClick: (id: string) => void
}

export function PositionCard({
  position,
  onPositionClick,
  className,
  type = 'button',
  ...props
}: PositionCardProps) {
  const elapsedTime = useElapsedTime(position.createdAt)

  return (
    <button
      className={classNames(styles.card, className)}
      data-position-id={position.id}
      data-status={position.status}
      onClick={() => onPositionClick(position.id)}
      type={type}
      {...props}
    >
      <span className={styles.body}>
        <span className={styles.title}>{position.title}</span>

        {position.comment ? (
          <span className={styles.comment}>
            <span className={styles.commentIcon} aria-hidden="true">
              !
            </span>
            <span>{position.comment}</span>
          </span>
        ) : null}
      </span>

      <span className={styles.meta}>
        <span className={styles.orderNumber}>{position.orderNumber}</span>
        <span className={styles.elapsed} aria-label={`Прошло ${elapsedTime} с создания`}>
          {elapsedTime}
        </span>
      </span>
    </button>
  )
}
