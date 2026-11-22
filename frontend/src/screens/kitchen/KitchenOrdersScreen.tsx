import { EmptyState, Icon, OrderPositionsGrid } from '../../components'
import { displayOrderNumber, orderKey } from '../../kitchen/orderKey'
import { formatTimer, slaLevel, useElapsedMs } from '../../components/PositionCard/elapsedTime'
import { classNames } from '../../components/classNames'
import { useKitchenBoard } from '../../kitchen/KitchenBoardContext'
import type { OrderPosition, OrderPositionStatus } from '../../types'
import styles from './KitchenOrdersScreen.module.css'

export type KitchenBoardScreenStatus = Extract<OrderPositionStatus, 'NEW' | 'IN_PROGRESS' | 'READY'>

type KitchenOrdersScreenProps = {
  /** The column shown on phones (tabs); on tablets/desktop all three columns are visible (audit B2). */
  statusFilter: KitchenBoardScreenStatus
}

const COLUMNS: Array<{ status: KitchenBoardScreenStatus; title: string; hint: string }> = [
  { status: 'NEW', title: 'Новые', hint: 'старые сверху' },
  { status: 'IN_PROGRESS', title: 'В работе', hint: 'по очереди' },
  { status: 'READY', title: 'Готовы к выдаче', hint: 'по заказам' },
]

type ReadyOrder = { key: string; orderNumber: string; ready: OrderPosition[]; waiting: OrderPosition[]; since: string }

function groupReadyOrders(ready: OrderPosition[], all: OrderPosition[]): ReadyOrder[] {
  const groups = new Map<string, ReadyOrder>()
  for (const position of ready) {
    const key = orderKey(position)
    const group = groups.get(key) ?? { key, orderNumber: position.orderNumber, ready: [], waiting: [], since: position.createdAt }
    group.ready.push(position)
    if (Date.parse(position.createdAt) < Date.parse(group.since)) group.since = position.createdAt
    groups.set(key, group)
  }
  for (const group of groups.values()) {
    group.waiting = all.filter(
      (position) => orderKey(position) === group.key && (position.status === 'NEW' || position.status === 'IN_PROGRESS'),
    )
  }
  return Array.from(groups.values()).sort((a, b) => Date.parse(a.since) - Date.parse(b.since))
}

function ReadyOrderCard({
  order,
  pending,
  onIssue,
}: {
  order: ReadyOrder
  pending: boolean
  onIssue: () => void
}) {
  const elapsedMs = useElapsedMs(order.since)
  const number = displayOrderNumber(order.orderNumber)

  return (
    <article
      className={styles.readyOrder}
      data-sla={slaLevel(elapsedMs)}
      aria-busy={pending || undefined}
      aria-label={`Заказ ${number}`}
    >
      <div className={styles.readyHead}>
        <span className={styles.bigNumber}>{number}</span>
        <span className={styles.readyMeta}>
          с заказа {formatTimer(elapsedMs)}
          {order.waiting.length > 0 ? <br /> : null}
          {order.waiting.length > 0 ? `ещё готовится: ${order.waiting.length}` : null}
        </span>
      </div>
      <ul className={styles.readyList}>
        {order.ready.map((position) => (
          <li key={position.id} data-position-id={position.id} data-status={position.status}>
            <Icon name="check" size={16} />
            {position.title}
          </li>
        ))}
        {order.waiting.map((position) => (
          <li key={position.id} className={styles.waitingItem}>
            <Icon name="clock" size={16} />
            {position.title}
          </li>
        ))}
      </ul>
      <button className={styles.issue} type="button" disabled={pending} onClick={onIssue}>
        <Icon name="hand" size={20} />
        {order.waiting.length > 0 ? 'Выдать готовое' : 'Выдать заказ'}
      </button>
    </article>
  )
}

export function KitchenOrdersScreen({ statusFilter }: KitchenOrdersScreenProps) {
  const {
    actionError,
    advancePosition,
    advancePositions,
    counts,
    error,
    loading,
    pendingPositionIds,
    positions,
    positionsByStatus,
  } = useKitchenBoard()

  if (loading) {
    return (
      <section aria-label={`${statusFilter} order positions`} data-status-filter={statusFilter}>
        <EmptyState title="Загружаем заказы" description="Позиции кухни появятся через секунду." />
      </section>
    )
  }

  if (error) {
    return (
      <section aria-label={`${statusFilter} order positions`} data-status-filter={statusFilter}>
        <EmptyState role="alert" title="Не удалось загрузить заказы" description={error} />
      </section>
    )
  }

  return (
    <div className={styles.screen} data-active-status={statusFilter}>
      {actionError ? (
        <div className={styles.actionError} role="alert">
          <Icon name="alertTriangle" size={18} />
          <span>
            <span className={styles.actionTitle}>Не удалось обновить статус</span>
            <span className={styles.actionDescription}>{actionError}</span>
          </span>
        </div>
      ) : null}

      <div className={styles.columns}>
        {COLUMNS.map((column) => {
          const columnPositions = positionsByStatus(column.status)
          const isActive = column.status === statusFilter
          return (
            <section
              key={column.status}
              className={classNames(styles.column, isActive ? styles.activeColumn : undefined)}
              data-column={column.status}
              aria-labelledby={`kitchen-col-${column.status}`}
            >
              <header className={styles.columnHead}>
                <h2 id={`kitchen-col-${column.status}`}>
                  {column.title}
                  <span className={styles.count}>{counts[column.status]}</span>
                </h2>
                <small>{column.hint}</small>
              </header>

              {column.status === 'READY' ? (
                <section
                  className={styles.readyColumn}
                  aria-label="READY order positions"
                  data-status-filter="READY"
                >
                  {columnPositions.length === 0 ? (
                    <EmptyState
                      className={styles.columnEmpty}
                      title="Пока пусто"
                      description="Готовые заказы соберутся здесь."
                    />
                  ) : (
                    groupReadyOrders(columnPositions, positions).map((order) => (
                      <ReadyOrderCard
                        key={order.key}
                        order={order}
                        pending={order.ready.some((position) => pendingPositionIds.includes(position.id))}
                        onIssue={() => {
                          void advancePositions(
                            order.ready.map((position) => position.id),
                            'READY',
                          )
                        }}
                      />
                    ))
                  )}
                </section>
              ) : (
                <OrderPositionsGrid
                  statusFilter={column.status}
                  positions={columnPositions}
                  allPositions={positions}
                  pendingPositionIds={pendingPositionIds}
                  onPositionClick={(id) => {
                    void advancePosition(id, column.status)
                  }}
                />
              )}
            </section>
          )
        })}
      </div>
    </div>
  )
}
