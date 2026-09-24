import { NavLink } from 'react-router-dom'

import { classNames } from '../components/classNames'
import { CartIcon, CatalogIcon, LocationIcon, ProfileIcon } from './NavIcons'
import styles from './BottomNav.module.css'

const NAV_ITEMS = [
  { to: '/locations', label: 'Адреса', Icon: LocationIcon },
  { to: '/catalog', label: 'Каталог', Icon: CatalogIcon },
  { to: '/cart', label: 'Корзина', Icon: CartIcon },
  { to: '/profile', label: 'Профиль', Icon: ProfileIcon },
]

export function BottomNav() {
  return (
    <nav className={styles.nav} aria-label="Основная навигация">
      {NAV_ITEMS.map(({ to, label, Icon }) => (
        <NavLink
          className={({ isActive }) => classNames(styles.item, isActive ? styles.active : undefined)}
          key={to}
          to={to}
        >
          <Icon aria-hidden="true" className={styles.icon} />
          <span className={styles.label}>{label}</span>
        </NavLink>
      ))}
    </nav>
  )
}
