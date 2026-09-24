import type { MouseEventHandler } from 'react'

import { classNames } from '../classNames'
import styles from './ListItem.module.css'

export type ListItemProps = {
  title: string
  address: string
  time: string
  marker?: boolean
  markerLabel?: string
  className?: string
  onClick?: MouseEventHandler<HTMLButtonElement | HTMLDivElement>
}

export function ListItem({
  title,
  address,
  time,
  marker = false,
  markerLabel = 'selected',
  className,
  onClick,
}: ListItemProps) {
  const content = (
    <>
      <div className={styles.content}>
        <p className={styles.title}>{title}</p>
        <p className={styles.meta}>{address}</p>
        <p className={styles.meta}>{time}</p>
      </div>
      {marker ? <span className={styles.marker} aria-label={markerLabel} role="img" /> : null}
    </>
  )

  if (onClick) {
    return (
      <button
        className={classNames(styles.item, styles.itemButton, className)}
        type="button"
        onClick={onClick}
      >
        {content}
      </button>
    )
  }

  return (
    <div className={classNames(styles.item, className)}>
      {content}
    </div>
  )
}
