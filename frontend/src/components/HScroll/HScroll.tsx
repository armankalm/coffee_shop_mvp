import type { HTMLAttributes, ReactNode } from 'react'

import { classNames } from '../classNames'
import styles from './HScroll.module.css'

export type HScrollProps = HTMLAttributes<HTMLDivElement> & {
  children: ReactNode
}

export function HScroll({ children, className, ...props }: HScrollProps) {
  return (
    <div className={classNames(styles.hScroll, 'hide-scrollbar', className)} {...props}>
      {children}
    </div>
  )
}
