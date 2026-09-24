import type { ButtonHTMLAttributes } from 'react'

import { Badge } from '../Badge'
import { classNames } from '../classNames'
import type { ProductBadge } from '../../types'
import styles from './ProductCard.module.css'

export type ProductCardProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & {
  imageAlt: string
  imageSrc: string
  title: string
  description?: string | null
  price: string
  badge?: ProductBadge
  isFavorite?: boolean
  onFavoriteToggle?: () => void
}

export function ProductCard({
  imageAlt,
  imageSrc,
  title,
  description,
  price,
  badge,
  isFavorite,
  onFavoriteToggle,
  className,
  type = 'button',
  ...props
}: ProductCardProps) {
  return (
    <article className={classNames(styles.card, className)}>
      <button className={styles.openButton} type={type} {...props}>
        <span className={styles.imageFrame}>
          <img className={styles.image} src={imageSrc} alt={imageAlt} draggable={false} />
          {badge ? (
            <Badge className={styles.badge} tone={badge.tone}>
              {badge.label}
            </Badge>
          ) : null}
        </span>
        <span className={styles.title}>{title}</span>
        {description ? <span className={styles.description}>{description}</span> : null}
        <span className={styles.footer}>
          <span className={styles.price}>{price}</span>
          <span className={styles.arrow} aria-hidden="true">
            &gt;
          </span>
        </span>
      </button>
      {onFavoriteToggle ? (
        <button
          aria-label={isFavorite ? 'Убрать из избранного' : 'Добавить в избранное'}
          aria-pressed={isFavorite}
          className={classNames(styles.favoriteButton, isFavorite ? styles.favoriteActive : undefined)}
          onClick={onFavoriteToggle}
          type="button"
        >
          {isFavorite ? '♥' : '♡'}
        </button>
      ) : null}
    </article>
  )
}
