import { EmptyState, OrderPositionsGrid } from '../../components'
import { useKitchenBoard } from '../../kitchen/KitchenBoardContext'
import type { OrderPositionStatus } from '../../types'
import styles from './KitchenOrdersScreen.module.css'

export type KitchenBoardScreenStatus = Extract<OrderPositionStatus, 'NEW' | 'IN_PROGRESS' | 'READY'>

type KitchenOrdersScreenProps = {
  statusFilter: KitchenBoardScreenStatus
}

export function KitchenOrdersScreen({ statusFilter }: KitchenOrdersScreenProps) {
  const { actionError, advancePosition, error, loading, pendingPositionIds, positionsByStatus } = useKitchenBoard()

  if (loading) {
    return (
      <section aria-label={`${statusFilter} order positions`} data-status-filter={statusFilter}>
        <EmptyState title="Loading orders" description="Kitchen positions are loading." />
      </section>
    )
  }

  if (error) {
    return (
      <section aria-label={`${statusFilter} order positions`} data-status-filter={statusFilter}>
        <EmptyState role="alert" title="Unable to load orders" description={error} />
      </section>
    )
  }

  const positions = positionsByStatus(statusFilter)

  return (
    <div className={styles.screen}>
      {actionError ? (
        <div className={styles.actionError} role="alert">
          <span className={styles.actionTitle}>Unable to update order</span>
          <span className={styles.actionDescription}>{actionError}</span>
        </div>
      ) : null}

      <OrderPositionsGrid
        statusFilter={statusFilter}
        positions={positions}
        pendingPositionIds={pendingPositionIds}
        onPositionClick={(id) => {
          void advancePosition(id, statusFilter)
        }}
      />
    </div>
  )
}
