import { useEffect, useState, type ReactNode } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'

import { Icon } from '../components'
import { classNames } from '../components/classNames'
import {
  KitchenBoardProvider,
  useKitchenBoard,
  type KitchenLastAction,
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

function useClock() {
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), 15_000)
    return () => window.clearInterval(id)
  }, [])
  return now.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
}

function UndoToast({
  action,
  durationMs,
  onUndo,
}: {
  action: KitchenLastAction | null
  durationMs: number
  onUndo: () => void
}) {
  return (
    <div className={styles.toastRegion} role="status" aria-live="polite">
      {action ? (
        <div className={styles.toast} key={action.key}>
          <span className={styles.toastText}>{action.message}</span>
          <button className={styles.toastUndo} type="button" onClick={onUndo}>
            <Icon name="undo" size={16} />
            Отменить
          </button>
          <span className={styles.toastBar} aria-hidden="true">
            <i style={{ animationDuration: `${durationMs}ms` }} />
          </span>
        </div>
      ) : null}
    </div>
  )
}

function KitchenLayoutContent() {
  const { counts, lastAction, undoLastAction, undoDelayMs } = useKitchenBoard()

  return (
    <KitchenLayoutShell counts={counts}>
      <UndoToast action={lastAction} durationMs={undoDelayMs} onUndo={undoLastAction} />
    </KitchenLayoutShell>
  )
}

export function KitchenLayoutShell({ counts, children }: { counts: PositionCounts; children?: ReactNode }) {
  const clock = useClock()

  return (
    <main className={classNames('safe-area', styles.layout)}>
      <div className={styles.shell}>
        <header className={styles.header}>
          <Link className={styles.back} to="/staff">
            <Icon name="left" size={16} />
            Рабочее место
          </Link>
          <h1 className={styles.title}>Заказы</h1>

          <div className={styles.legend} aria-label="Легенда таймеров">
            <span>
              <i className={styles.dotOk} />
              до 3 мин
            </span>
            <span>
              <i className={styles.dotWarn} />
              3–6 мин
            </span>
            <span>
              <i className={styles.dotLate} />
              больше 6 мин
            </span>
          </div>
          <time className={styles.clock}>{clock}</time>
        </header>

        <nav className={styles.tabs} aria-label="Статусы заказов">
          {KITCHEN_TABS.map((tab) => (
            <NavLink
              className={({ isActive }) => classNames(styles.tab, isActive ? styles.activeTab : undefined)}
              end
              key={tab.to}
              to={tab.to}
            >
              {tab.label}
              <span className={styles.tabCount}>{counts[tab.status]}</span>
            </NavLink>
          ))}
        </nav>

        <div className={styles.content}>
          <Outlet />
        </div>
      </div>
      {children}
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
