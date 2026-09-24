import type { CSSProperties, ReactNode } from 'react'

import { classNames } from '../classNames'
import styles from './Skeleton.module.css'

export type SkeletonProps = {
  width?: CSSProperties['width']
  height?: CSSProperties['height']
  radius?: CSSProperties['borderRadius']
  className?: string | undefined
}

/** A single shimmering placeholder block. */
export function Skeleton({ width = '100%', height = 14, radius = 8, className }: SkeletonProps) {
  return <span className={classNames(styles.block, className)} style={{ width, height, borderRadius: radius }} />
}

export type SkeletonStatusProps = {
  /** Announced to screen readers instead of the visual placeholders, e.g. "Загружаем меню". */
  label: string
  className?: string | undefined
  children: ReactNode
}

/** Wraps placeholders in a live status region so assistive tech hears what is loading. */
export function SkeletonStatus({ label, className, children }: SkeletonStatusProps) {
  return (
    <div className={classNames(styles.status, className)} role="status" aria-live="polite" aria-busy="true">
      <span className={styles.srOnly}>{label}</span>
      <div aria-hidden="true" className={styles.content}>
        {children}
      </div>
    </div>
  )
}

export type SkeletonRowsProps = {
  label: string
  count?: number
  /** Leading square thumbnail, as in product or shop rows. */
  thumb?: boolean
  /** Trailing short block, e.g. a price. */
  trailing?: boolean
  className?: string | undefined
}

/** A vertical list of rows: optional thumbnail, a title and a subtitle line. */
export function SkeletonRows({ label, count = 5, thumb = true, trailing = true, className }: SkeletonRowsProps) {
  return (
    <SkeletonStatus label={label} className={className}>
      <div className={styles.rows}>
        {Array.from({ length: count }, (_, index) => (
          <div className={styles.row} key={index}>
            {thumb ? <Skeleton width={44} height={44} radius={10} /> : null}
            <div className={styles.rowText}>
              <Skeleton width={`${55 + ((index * 17) % 30)}%`} height={14} />
              <Skeleton width="35%" height={12} />
            </div>
            {trailing ? <Skeleton width={56} height={14} /> : null}
          </div>
        ))}
      </div>
    </SkeletonStatus>
  )
}

export type SkeletonCardsProps = {
  label: string
  count?: number
  /** The grid class of the real list, so placeholders take the same columns. */
  gridClassName?: string | undefined
}

/** Product-card placeholders: image, name and price. */
export function SkeletonCards({ label, count = 6, gridClassName }: SkeletonCardsProps) {
  return (
    <SkeletonStatus label={label}>
      <div className={gridClassName ?? styles.cards}>
        {Array.from({ length: count }, (_, index) => (
          <div className={styles.card} key={index}>
            <Skeleton height="auto" radius={16} className={styles.cardImage} />
            <Skeleton width="75%" height={14} />
            <Skeleton width="40%" height={12} />
          </div>
        ))}
      </div>
    </SkeletonStatus>
  )
}

export type SkeletonFormProps = {
  label: string
  fields?: number
  /** Leading image block, as on the product editor. */
  image?: boolean
}

/** Form placeholders: a label line and an input box per field, then a button. */
export function SkeletonForm({ label, fields = 4, image = false }: SkeletonFormProps) {
  return (
    <SkeletonStatus label={label}>
      <div className={styles.form}>
        {image ? <Skeleton width={112} height={112} radius={16} /> : null}
        {Array.from({ length: fields }, (_, index) => (
          <div className={styles.field} key={index}>
            <Skeleton width={90} height={12} />
            <Skeleton height={46} radius={12} />
          </div>
        ))}
        <Skeleton width={140} height={44} radius={12} />
      </div>
    </SkeletonStatus>
  )
}
