import { useEffect } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'

import { useCart } from '../cart/CartContext'
import { Icon } from '../components'
import { classNames } from '../components/classNames'
import { formatMoney, pluralPositions } from '../format'
import { useShop } from '../shop/ShopContext'
import { ToastProvider } from '../toast/ToastContext'
import { BottomNav } from './BottomNav'
import styles from './AppLayout.module.css'

const ROUTES_WITHOUT_NAV = [/^\/login/, /^\/auth\//, /^\/product\//]
/** Screens that show the floating «N позиций · Оформить» bar above the tab bar. */
const ROUTES_WITH_CART_BAR = [/^\/$/, /^\/home$/, /^\/catalog$/]

export function AppLayout() {
  const location = useLocation()
  const { shop } = useShop()
  const { lines } = useCart()

  // Variant B «Cream» is the customer theme; kitchen/staff screens live outside this layout.
  useEffect(() => {
    const root = document.documentElement
    root.dataset.theme = 'cream'
    return () => {
      delete root.dataset.theme
    }
  }, [])

  const showNav = !ROUTES_WITHOUT_NAV.some((pattern) => pattern.test(location.pathname))
  const shopLines = shop ? lines.filter((line) => line.shopId === shop.id) : []
  const itemCount = shopLines.reduce((sum, line) => sum + line.quantity, 0)
  const total = shopLines.reduce((sum, line) => sum + (line.basePrice + line.toppingsPrice) * line.quantity, 0)
  const showCartBar = showNav && itemCount > 0 && ROUTES_WITH_CART_BAR.some((pattern) => pattern.test(location.pathname))

  return (
    <ToastProvider>
      <main className={classNames('safe-area', styles.app)}>
        <div className={styles.shell}>
          <div
            className={classNames(
              styles.content,
              showNav ? styles.contentWithNav : undefined,
              showCartBar ? styles.contentWithCartBar : undefined,
            )}
          >
            <Outlet />
          </div>
        </div>
        {showCartBar ? (
          <Link className={styles.cartBar} to="/cart">
            <span>{pluralPositions(itemCount)}</span>
            <span className={styles.cartBarAction}>
              Оформить · {formatMoney(total)}
              <Icon name="right" size={18} />
            </span>
          </Link>
        ) : null}
        {showNav ? <BottomNav cartCount={itemCount} /> : null}
      </main>
    </ToastProvider>
  )
}
