import { Link, useLocation } from 'react-router-dom'

import type { CoffeeShopDto } from '../api/shops'
import { Icon } from '../components'
import { isShopOpen } from '../shop/shopStatus'
import styles from './Cream.module.css'

/** «📍 Абая 52 · Открыто ▾» — the shop is a context setting in the header, not a tab (audit N1). */
export function ShopLine({ shop }: { shop: CoffeeShopDto }) {
  const location = useLocation()
  const open = isShopOpen(shop)

  return (
    <Link
      className={styles.shopLine}
      to="/locations"
      state={{ from: location.pathname }}
      aria-label={`Кофейня: ${shop.name}, ${shop.statusNameRu}. Сменить`}
    >
      <Icon name="pin" size={16} />
      <b>{shop.name}</b>
      <span aria-hidden="true">·</span>
      <span className={open ? styles.statusOpen : styles.statusClosed}>{shop.statusNameRu}</span>
      <Icon name="down" size={16} />
    </Link>
  )
}
