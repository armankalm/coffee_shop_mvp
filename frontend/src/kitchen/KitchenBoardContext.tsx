import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'

import {
  advancePositionStatus,
  getPositions,
  nextStatus,
  openKitchenBoardStream,
  toOrderPosition,
  type OrderItemBoardDto,
} from '../api/positions'
import { getCurrentUser } from '../api/user'
import type { OrderPosition, OrderPositionStatus } from '../types'
import { displayOrderNumber } from './orderKey'

const SELECTED_SHOP_KEY = 'drinkit.staff.shopId'

function readSelectedShopId(): number | null {
  const raw = localStorage.getItem(SELECTED_SHOP_KEY)
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isFinite(parsed) ? parsed : null
}

/**
 * The shop whose kitchen board to stream: the staff-selected shop if set,
 * otherwise the first shop assigned to the current user. This keeps live
 * updates working even if the barista opened /orders/* without visiting /staff.
 */
async function resolveKitchenShopId(): Promise<number | null> {
  const stored = readSelectedShopId()
  if (stored != null) return stored

  try {
    const me = await getCurrentUser()
    return me.assignedShops?.[0]?.id ?? me.coffeeShopId ?? null
  } catch {
    return null
  }
}

export type PositionCounts = Record<OrderPositionStatus, number>

/** The last status change, kept for UNDO_DELAY_MS so the barista can undo a mis-tap (audit B1). */
export type KitchenLastAction = {
  key: number
  positionIds: string[]
  message: string
  toStatus: OrderPositionStatus
}

/** The backend has no backward transitions, so a change is sent only after the undo window closes. */
export const UNDO_DELAY_MS = 5000

type KitchenBoardContextValue = {
  positions: OrderPosition[]
  loading: boolean
  error: string | null
  actionError: string | null
  pendingPositionIds: readonly string[]
  advancePosition: (id: string, expectedStatus?: OrderPositionStatus) => Promise<void>
  /** Advances several positions at once, e.g. «Выдать заказ» for every ready item of one order. */
  advancePositions: (ids: string[], expectedStatus: OrderPositionStatus) => Promise<void>
  lastAction: KitchenLastAction | null
  undoLastAction: () => void
  undoDelayMs: number
  positionsByStatus: (status: OrderPositionStatus) => OrderPosition[]
  counts: PositionCounts
}

const STATUS_LABELS: Record<OrderPositionStatus, string> = {
  NEW: 'Новые',
  IN_PROGRESS: 'В работе',
  READY: 'Готов',
  COMPLETED: 'Выдан',
}

const KitchenBoardContext = createContext<KitchenBoardContextValue | null>(null)

export function selectPositionsByStatus(positions: OrderPosition[], status: OrderPositionStatus) {
  return positions
    .filter((position) => position.status === status)
    .sort((first, second) => Date.parse(first.createdAt) - Date.parse(second.createdAt))
}

export function countPositionsByStatus(positions: OrderPosition[]): PositionCounts {
  return positions.reduce<PositionCounts>(
    (counts, position) => ({
      ...counts,
      [position.status]: counts[position.status] + 1,
    }),
    {
      NEW: 0,
      IN_PROGRESS: 0,
      READY: 0,
      COMPLETED: 0,
    },
  )
}

export function advancePositionOptimistically(positions: OrderPosition[], id: string) {
  return positions.map((position) =>
    position.id === id ? { ...position, status: nextStatus(position.status) } : position,
  )
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Failed to update order position'
}

type ScheduledChange = { timer: number | null; previous: OrderPosition[] }

export function KitchenBoardProvider({
  children,
  undoDelayMs = UNDO_DELAY_MS,
}: {
  children: ReactNode
  /** 0 sends status changes immediately (no undo window). */
  undoDelayMs?: number
}) {
  const [positions, setPositions] = useState<OrderPosition[]>([])
  const [lastAction, setLastAction] = useState<KitchenLastAction | null>(null)
  const scheduledRef = useRef<Map<number, ScheduledChange>>(new Map())
  const nextActionKey = useRef(1)
  const positionsRef = useRef<OrderPosition[]>([])
  const pendingPositionIdsRef = useRef<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [pendingPositionIds, setPendingPositionIds] = useState<string[]>([])

  const updatePositions = useCallback((nextPositions: OrderPosition[]) => {
    positionsRef.current = nextPositions
    setPositions(nextPositions)
  }, [])

  const setPositionPending = useCallback((id: string, pending: boolean) => {
    const nextPendingPositionIds = new Set(pendingPositionIdsRef.current)

    if (pending) {
      nextPendingPositionIds.add(id)
    } else {
      nextPendingPositionIds.delete(id)
    }

    pendingPositionIdsRef.current = nextPendingPositionIds
    setPendingPositionIds([...nextPendingPositionIds])
  }, [])

  useEffect(() => {
    let cancelled = false

    getPositions()
      .then((nextPositions) => {
        if (!cancelled) updatePositions(nextPositions)
      })
      .catch((loadError) => {
        if (!cancelled) setError(getErrorMessage(loadError))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [updatePositions])

  // Live updates: subscribe to the shop's kitchen board over SSE so new orders
  // (e.g. placed at the POS) and status changes appear without a reload.
  useEffect(() => {
    let source: EventSource | null = null
    let closed = false

    resolveKitchenShopId().then((shopId) => {
      if (closed || shopId == null) return

      source = openKitchenBoardStream(shopId)

      source.addEventListener('items', (event) => {
        // Skip while an optimistic advance is in flight, so the server snapshot
        // doesn't briefly overwrite the pending card before it's confirmed.
        if (pendingPositionIdsRef.current.size > 0) return

        try {
          const items = JSON.parse((event as MessageEvent).data) as OrderItemBoardDto[]
          updatePositions(items.map(toOrderPosition))
          setLoading(false)
        } catch {
          // Ignore malformed frames; the next snapshot will reconcile.
        }
      })
    })

    return () => {
      closed = true
      source?.close()
    }
  }, [updatePositions])

  /** Sends the scheduled change of one action to the server, rolling back on failure. */
  const commit = useCallback(
    async (previous: OrderPosition[]) => {
      await Promise.all(
        previous.map(async (previousPosition) => {
          const id = previousPosition.id
          try {
            const updatedPosition = await advancePositionStatus(id, previousPosition.status)
            updatePositions(positionsRef.current.map((position) => (position.id === id ? updatedPosition : position)))
          } catch (advanceError) {
            updatePositions(positionsRef.current.map((position) => (position.id === id ? previousPosition : position)))
            setActionError(getErrorMessage(advanceError))
          } finally {
            setPositionPending(id, false)
          }
        }),
      )
    },
    [setPositionPending, updatePositions],
  )

  const advancePositions = useCallback(
    async (ids: string[], expectedStatus: OrderPositionStatus) => {
      const previous = ids
        .filter((id) => !pendingPositionIdsRef.current.has(id))
        .map((id) => positionsRef.current.find((position) => position.id === id))
        .filter((position): position is OrderPosition => position != null && position.status === expectedStatus)

      if (previous.length === 0) return

      setActionError(null)
      let next = positionsRef.current
      for (const position of previous) {
        setPositionPending(position.id, true)
        next = advancePositionOptimistically(next, position.id)
      }
      updatePositions(next)

      if (undoDelayMs <= 0) {
        await commit(previous)
        return
      }

      const key = nextActionKey.current++
      const toStatus = nextStatus(expectedStatus)
      const first = previous[0]!
      const number = displayOrderNumber(first.orderNumber)
      const what = previous.length === 1 ? `${number} ${first.title}` : `${number} · ${previous.length} поз.`
      const change: ScheduledChange = { timer: null, previous }
      change.timer = window.setTimeout(() => {
        scheduledRef.current.delete(key)
        setLastAction((current) => (current?.key === key ? null : current))
        void commit(previous)
      }, undoDelayMs)
      scheduledRef.current.set(key, change)
      setLastAction({ key, positionIds: previous.map((p) => p.id), message: `${what} → ${STATUS_LABELS[toStatus]}`, toStatus })
    },
    [commit, setPositionPending, undoDelayMs, updatePositions],
  )

  const advancePosition = useCallback(
    async (id: string, expectedStatus?: OrderPositionStatus) => {
      const status = expectedStatus ?? positionsRef.current.find((position) => position.id === id)?.status
      if (!status) return
      await advancePositions([id], status)
    },
    [advancePositions],
  )

  const undoLastAction = useCallback(() => {
    if (!lastAction) return
    const change = scheduledRef.current.get(lastAction.key)
    setLastAction(null)
    if (!change) return
    if (change.timer != null) window.clearTimeout(change.timer)
    scheduledRef.current.delete(lastAction.key)
    const restore = new Map(change.previous.map((position) => [position.id, position]))
    updatePositions(positionsRef.current.map((position) => restore.get(position.id) ?? position))
    for (const position of change.previous) setPositionPending(position.id, false)
  }, [lastAction, setPositionPending, updatePositions])

  // Leaving the board must not lose taps still inside their undo window: send them right away.
  useEffect(() => {
    const scheduled = scheduledRef.current
    return () => {
      for (const change of scheduled.values()) {
        if (change.timer != null) window.clearTimeout(change.timer)
        void commit(change.previous)
      }
      scheduled.clear()
    }
  }, [commit])

  const counts = useMemo(() => countPositionsByStatus(positions), [positions])

  const positionsByStatus = useCallback(
    (status: OrderPositionStatus) => selectPositionsByStatus(positions, status),
    [positions],
  )

  const value = useMemo<KitchenBoardContextValue>(
    () => ({
      positions,
      loading,
      error,
      actionError,
      pendingPositionIds,
      advancePosition,
      advancePositions,
      lastAction,
      undoLastAction,
      undoDelayMs,
      positionsByStatus,
      counts,
    }),
    [
      positions,
      loading,
      error,
      actionError,
      pendingPositionIds,
      advancePosition,
      advancePositions,
      lastAction,
      undoLastAction,
      undoDelayMs,
      positionsByStatus,
      counts,
    ],
  )

  return <KitchenBoardContext.Provider value={value}>{children}</KitchenBoardContext.Provider>
}

export function useKitchenBoard() {
  const context = useContext(KitchenBoardContext)
  if (!context) {
    throw new Error('useKitchenBoard must be used within a KitchenBoardProvider')
  }
  return context
}
