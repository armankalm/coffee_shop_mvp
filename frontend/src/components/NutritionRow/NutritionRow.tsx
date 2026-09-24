import type { HTMLAttributes } from 'react'

import { classNames } from '../classNames'
import styles from './NutritionRow.module.css'

type NutritionValue = number | string

export type NutritionRowProps = HTMLAttributes<HTMLDivElement> & {
  calories: NutritionValue
  fats: NutritionValue
  carbs: NutritionValue
  proteins: NutritionValue
}

export function NutritionRow({ calories, fats, carbs, proteins, className, ...props }: NutritionRowProps) {
  const metrics = [
    { label: 'ккал', value: calories },
    { label: 'жиры', value: fats },
    { label: 'углеводы', value: carbs },
    { label: 'белки', value: proteins },
  ]

  return (
    <div className={classNames(styles.row, className)} {...props}>
      {metrics.map((metric) => (
        <div className={styles.metric} key={metric.label}>
          <span className={styles.value}>{metric.value}</span>
          <span className={styles.label}>{metric.label}</span>
        </div>
      ))}
    </div>
  )
}
