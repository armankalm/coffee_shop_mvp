import type { HTMLAttributes, ReactNode } from 'react'

import type { BadgeTone } from '../../types'
import { classNames } from '../classNames'
import styles from './Badge.module.css'

export type { BadgeTone }

export type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: BadgeTone
  children: ReactNode
}

export function Badge({ tone = 'blue', className, children, ...props }: BadgeProps) {
  return (
    <span className={classNames(styles.badge, styles[tone], className)} {...props}>
      {children}
    </span>
  )
}
