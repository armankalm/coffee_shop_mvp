import { AnimatePresence, motion } from 'framer-motion'

import type { OrderPosition, OrderPositionStatus } from '../../types'
import { EmptyState } from '../EmptyState'
import { PositionCard } from '../PositionCard'
import styles from './OrderPositionsGrid.module.css'

export type OrderPositionsGridProps = {
  statusFilter: OrderPositionStatus
  positions: OrderPosition[]
  pendingPositionIds?: readonly string[]
  onPositionClick: (id: string) => void
}

export function OrderPositionsGrid({
  statusFilter,
  positions,
  pendingPositionIds = [],
  onPositionClick,
}: OrderPositionsGridProps) {
  const isEmpty = positions.length === 0

  return (
    <section
      className={styles.grid}
      aria-label={`${statusFilter} order positions`}
      data-status-filter={statusFilter}
    >
      {isEmpty ? (
        <EmptyState
          className={styles.emptyState}
          title="Очередь пуста"
          description="Новые позиции появятся здесь автоматически."
        />
      ) : (
        <AnimatePresence initial={false} mode="popLayout">
          {positions.map((position) => {
            const isPending = pendingPositionIds.includes(position.id)

            return (
              <motion.div
                key={position.id}
                className={styles.gridItem}
                data-position-layout={position.id}
                layout
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.96 }}
                transition={{ duration: 0.18, ease: 'easeOut' }}
              >
                <PositionCard
                  position={position}
                  aria-busy={isPending}
                  disabled={isPending}
                  onPositionClick={onPositionClick}
                />
              </motion.div>
            )
          })}
        </AnimatePresence>
      )}
    </section>
  )
}
