import type { HTMLAttributes } from 'react'

import { classNames } from '../classNames'
import styles from './Stepper.module.css'

export type StepperProps = HTMLAttributes<HTMLDivElement> & {
  value: number
  min?: number
  max?: number
  decreaseLabel?: string
  increaseLabel?: string
  onDecrease?: () => void
  onIncrease?: () => void
}

export function Stepper({
  value,
  min = 0,
  max,
  decreaseLabel = 'Decrease quantity',
  increaseLabel = 'Increase quantity',
  onDecrease,
  onIncrease,
  className,
  ...props
}: StepperProps) {
  const isMin = value <= min
  const isMax = max === undefined ? false : value >= max

  return (
    <div className={classNames(styles.stepper, className)} {...props}>
      <button
        className={styles.control}
        type="button"
        onClick={onDecrease}
        disabled={isMin}
        aria-label={decreaseLabel}
      >
        -
      </button>
      <span className={styles.value} aria-live="polite">
        {value}
      </span>
      <button
        className={styles.control}
        type="button"
        onClick={onIncrease}
        disabled={isMax}
        aria-label={increaseLabel}
      >
        +
      </button>
    </div>
  )
}
