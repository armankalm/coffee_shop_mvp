import type { HTMLAttributes } from 'react'

import { classNames } from '../classNames'
import styles from './EmptyState.module.css'

export type EmptyStateProps = HTMLAttributes<HTMLDivElement> & {
  title: string
  description?: string
}

export function EmptyState({ title, description, className, ...props }: EmptyStateProps) {
  return (
    <div className={classNames(styles.emptyState, className)} role="status" {...props}>
      <span className={styles.illustration} aria-hidden="true">
        <span className={styles.ticket} />
        <span className={styles.line} />
        <span className={styles.shortLine} />
      </span>

      <span className={styles.copy}>
        <span className={styles.title}>{title}</span>
        {description ? <span className={styles.description}>{description}</span> : null}
      </span>
    </div>
  )
}
