import { Outlet, useLocation } from 'react-router-dom'

import { classNames } from '../components/classNames'
import { BottomNav } from './BottomNav'
import styles from './AppLayout.module.css'

export function AppLayout() {
  const location = useLocation()
  const showNav = location.pathname !== '/login'

  return (
    <main className={classNames('safe-area', styles.app)}>
      <div className={styles.shell}>
        <div className={classNames(styles.content, showNav ? styles.contentWithNav : undefined)}>
          <Outlet />
        </div>
      </div>
      {showNav ? <BottomNav /> : null}
    </main>
  )
}
