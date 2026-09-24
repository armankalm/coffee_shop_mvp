import type { HTMLAttributes } from 'react'

import { classNames } from '../classNames'
import styles from './CrossSellCard.module.css'

export type CrossSellCardProps = HTMLAttributes<HTMLElement> & {
  imageAlt: string
  imageSrc: string
  title: string
  price: string
  addLabel?: string
  onAdd?: () => void
}

export function CrossSellCard({
  imageAlt,
  imageSrc,
  title,
  price,
  addLabel,
  onAdd,
  className,
  ...props
}: CrossSellCardProps) {
  return (
    <article className={classNames(styles.card, className)} {...props}>
      <div className={styles.imageFrame}>
        <img className={styles.image} src={imageSrc} alt={imageAlt} draggable={false} />
      </div>
      <div className={styles.body}>
        <p className={styles.title}>{title}</p>
        <div className={styles.footer}>
          <span className={styles.price}>{price}</span>
          <button className={styles.addButton} type="button" onClick={onAdd} aria-label={addLabel ?? `Add ${title}`}>
            +
          </button>
        </div>
      </div>
    </article>
  )
}
