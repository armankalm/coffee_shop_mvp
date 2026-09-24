import { useParams } from 'react-router-dom'

import type { BoardOrder } from '../../api/board'
import { pageForTick, useBoardTick } from '../../board/useAutoRotatingPage'
import { useOrderBoard } from '../../board/useOrderBoard'
import { classNames } from '../../components/classNames'
import styles from './OrderBoardScreen.module.css'

const PAGE_SIZE = 6
const ROTATE_INTERVAL_MS = 10000

function PageProgress({
  pageCount,
  pageIndex,
  progress,
}: {
  pageCount: number
  pageIndex: number
  progress: number
}) {
  if (pageCount <= 1) {
    return <div className={styles.progress} aria-hidden="true" />
  }

  return (
    <div className={styles.progress} aria-hidden="true">
      {Array.from({ length: pageCount }, (_, index) => {
        const fill = index < pageIndex ? 1 : index === pageIndex ? progress : 0
        return (
          <span key={index} className={styles.progressTrack}>
            <span className={styles.progressFill} style={{ transform: `scaleX(${fill})` }} />
          </span>
        )
      })}
    </div>
  )
}

type BoardColumnProps = {
  title: string
  orders: BoardOrder[]
  emptyLabel: string
  tick: number
  progress: number
}

function BoardColumn({ title, orders, emptyLabel, tick, progress }: BoardColumnProps) {
  // Each column paginates independently: its own page count derived from its
  // own item count, driven by the shared tick.
  const { items, pageIndex, pageCount } = pageForTick(orders, PAGE_SIZE, tick)

  return (
    <section className={styles.column}>
      <h2 className={styles.columnTitle}>{title}</h2>

      {orders.length === 0 ? (
        <p className={styles.empty}>{emptyLabel}</p>
      ) : (
        // Re-key by page so switching animates. The animation starts fully
        // opaque (only a slight rise), so there is never an empty/blank frame.
        <ul key={pageIndex} className={styles.list}>
          {items.map((order) => (
            <li key={order.orderId} className={styles.row}>
              <span
                className={classNames(
                  styles.badge,
                  order.status === 'READY' ? styles.badgeReady : undefined,
                )}
              >
                {/* Board shows the bare number (no "№" prefix). */}
                {order.dailyNumber ?? order.orderNumber.replace(/^№/, '')}
              </span>
              <span className={styles.name}>{order.customerName}</span>
            </li>
          ))}
        </ul>
      )}

      <PageProgress pageCount={pageCount} pageIndex={pageIndex} progress={progress} />
    </section>
  )
}

export function OrderBoardScreen() {
  const { shopId } = useParams<{ shopId: string }>()
  const parsedShopId = shopId ? Number(shopId) : null
  const validShopId = parsedShopId != null && Number.isFinite(parsedShopId) ? parsedShopId : null

  const { orders, connected } = useOrderBoard(validShopId)

  const inProgress = orders.filter(
    (order) => order.status === 'NEW' || order.status === 'IN_PROGRESS',
  )
  const ready = orders.filter((order) => order.status === 'READY')

  // Rotate while either column overflows one page; each column still computes
  // its own page count and slice from the shared tick.
  const needsRotation = inProgress.length > PAGE_SIZE || ready.length > PAGE_SIZE
  const { tick, progress } = useBoardTick(ROTATE_INTERVAL_MS, needsRotation)

  if (validShopId == null) {
    return (
      <main className={styles.board}>
        <p className={styles.empty}>Не указана кофейня. Откройте /board/&lt;id&gt;.</p>
      </main>
    )
  }

  return (
    <main className={styles.board}>
      <div className={styles.columns}>
        <BoardColumn
          title="В работе"
          orders={inProgress}
          emptyLabel="Нет заказов в работе"
          tick={tick}
          progress={progress}
        />
        <BoardColumn
          title="Готовы"
          orders={ready}
          emptyLabel="Нет готовых заказов"
          tick={tick}
          progress={progress}
        />
      </div>

      <span
        className={connected ? styles.statusOnline : styles.statusOffline}
        aria-label={connected ? 'Соединение активно' : 'Соединение потеряно'}
        title={connected ? 'Онлайн' : 'Переподключение…'}
      />
    </main>
  )
}
