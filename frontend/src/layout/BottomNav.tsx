import { NavLink } from 'react-router-dom'

import { Icon, type IconName } from '../components'
import { classNames } from '../components/classNames'
import styles from './BottomNav.module.css'

const NAV_ITEMS: Array<{ to: string; label: string; icon: IconName; end?: boolean }> = [
  { to: '/home', label: 'Главная', icon: 'home' },
  { to: '/catalog', label: 'Меню', icon: 'cup' },
  { to: '/cart', label: 'Корзина', icon: 'bag' },
  { to: '/profile', label: 'Профиль', icon: 'user' },
]

export function BottomNav({ cartCount = 0 }: { cartCount?: number }) {
  return (
    <nav className={styles.nav} aria-label="Основная навигация">
      {NAV_ITEMS.map(({ to, label, icon }) => (
        <NavLink
          className={({ isActive }) => classNames(styles.item, isActive ? styles.active : undefined)}
          key={to}
          to={to}
        >
          <Icon name={icon} size={22} className={styles.icon} />
          <span className={styles.label}>{label}</span>
          {to === '/cart' && cartCount > 0 ? (
            <span className={styles.badge}>{cartCount > 99 ? '99+' : cartCount}</span>
          ) : null}
        </NavLink>
      ))}
    </nav>
  )
}
