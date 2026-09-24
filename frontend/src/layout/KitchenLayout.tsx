import { Link, NavLink, Outlet } from 'react-router-dom'

import { classNames } from '../components/classNames'
import {
  KitchenBoardProvider,
  useKitchenBoard,
  type PositionCounts,
} from '../kitchen/KitchenBoardContext'
import type { OrderPositionStatus } from '../types'
import styles from './KitchenLayout.module.css'

type KitchenTab = {
  to: string
  label: string
  status: Extract<OrderPositionStatus, 'NEW' | 'IN_PROGRESS' | 'READY'>
}

const KITCHEN_TABS: KitchenTab[] = [
  { to: '/orders/new', label: 'Новые', status: 'NEW' },
  { to: '/orders/in-progress', label: 'В работе', status: 'IN_PROGRESS' },
  { to: '/orders/ready', label: 'Готовы', status: 'READY' },
]

function KitchenLayoutContent() {
  const { counts } = useKitchenBoard()

  return <KitchenLayoutShell counts={counts} />
}

export function KitchenLayoutShell({ counts }: { counts: PositionCounts }) {
  return (
    <main className={classNames('safe-area', styles.layout)}>
      <div className={styles.shell}>
        <header className={styles.header}>
          <div>
            <Link className={styles.eyebrow} to="/staff">
              ← Рабочее место
            </Link>
            <h1 className={styles.title}>Заказы</h1>
          </div>

          <nav className={styles.tabs} aria-label="Статусы заказов">
            {KITCHEN_TABS.map((tab) => (
              <NavLink
                className={({ isActive }) => classNames(styles.tab, isActive ? styles.activeTab : undefined)}
                end
                key={tab.to}
                to={tab.to}
              >
                {tab.label} · {counts[tab.status]}
              </NavLink>
            ))}
          </nav>
        </header>

        <div className={styles.content}>
          <Outlet />
        </div>
      </div>
    </main>
  )
}

export function KitchenLayout() {
  return (
    <KitchenBoardProvider>
      <KitchenLayoutContent />
    </KitchenBoardProvider>
  )
}
