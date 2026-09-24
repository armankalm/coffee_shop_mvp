import { useEffect, useRef, useState } from 'react'

import { openOrderBoardStream, type BoardOrder } from '../api/board'

export type OrderBoardState = {
  orders: BoardOrder[]
  connected: boolean
  error: string | null
}

/**
 * Subscribes to the live pickup board for a shop over SSE.
 * Keeps the latest full snapshot; the browser's EventSource reconnects
 * automatically, and we surface the connection state for the UI.
 */
export function useOrderBoard(shopId: number | null): OrderBoardState {
  const [orders, setOrders] = useState<BoardOrder[]>([])
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const sourceRef = useRef<EventSource | null>(null)

  useEffect(() => {
    if (shopId == null) {
      return
    }

    const source = openOrderBoardStream(shopId)
    sourceRef.current = source

    source.onopen = () => {
      setConnected(true)
      setError(null)
    }

    source.addEventListener('board', (event) => {
      try {
        setOrders(JSON.parse((event as MessageEvent).data) as BoardOrder[])
      } catch {
        setError('Не удалось прочитать данные табло')
      }
    })

    source.onerror = () => {
      // EventSource retries on its own; reflect the transient drop in the UI.
      setConnected(false)
    }

    return () => {
      source.close()
      sourceRef.current = null
    }
  }, [shopId])

  return { orders, connected, error }
}
