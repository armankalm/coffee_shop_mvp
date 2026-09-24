import type { ButtonHTMLAttributes, ReactNode } from 'react'

import { classNames } from '../classNames'
import styles from './Button.module.css'

export type ButtonVariant = 'primary' | 'light' | 'pill' | 'fab'

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  children: ReactNode
}

export function Button({
  variant = 'primary',
  className,
  children,
  type = 'button',
  ...props
}: ButtonProps) {
  return (
    <button className={classNames(styles.button, styles[variant], className)} type={type} {...props}>
      {children}
    </button>
  )
}
