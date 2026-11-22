import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'

import { orderKey } from '../../kitchen/orderKey'
import type { OrderPosition, OrderPositionStatus } from '../../types'
import { EmptyState } from '../EmptyState'
import { PositionCard } from '../PositionCard'
import styles from './OrderPositionsGrid.module.css'

export type OrderPositionsGridProps = {
  statusFilter: OrderPositionStatus
  positions: OrderPosition[]
  /** Every board position, to show the other items of the same order on each card. */
  allPositions?: OrderPosition[]
  pendingPositionIds?: readonly string[]
  onPositionClick: (id: string) => void
}

export function OrderPositionsGrid({
  statusFilter,
  positions,
  allPositions = [],
  pendingPositionIds = [],
  onPositionClick,
}: OrderPositionsGridProps) {
  const reduceMotion = useReducedMotion()
  const isEmpty = positions.length === 0

  return (
    <section className={styles.grid} aria-label={`${statusFilter} order positions`} data-status-filter={statusFilter}>
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
            const siblings = allPositions.filter(
              (other) => other.id !== position.id && other.status !== 'COMPLETED' && orderKey(other) === orderKey(position),
            )

            return (
              <motion.div
                key={position.id}
                className={styles.gridItem}
                data-position-layout={position.id}
                layout={!reduceMotion}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.96 }}
                transition={{ duration: reduceMotion ? 0 : 0.18, ease: 'easeOut' }}
              >
                <PositionCard
                  position={position}
                  siblings={siblings}
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
