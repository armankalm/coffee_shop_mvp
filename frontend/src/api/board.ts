import { API_BASE_URL, readAccessTokenFromStorage } from './client'

export type BoardOrderStatus = 'NEW' | 'IN_PROGRESS' | 'READY'

export type BoardOrder = {
  orderId: number
  /** Per-shop daily number (null for legacy orders). */
  dailyNumber: number | null
  /** Display label already including the "№" prefix, e.g. "№7". */
  orderNumber: string
  customerName: string
  status: BoardOrderStatus
}

/**
 * Opens an SSE connection to the live pickup board for a shop.
 * The backend emits a "board" event carrying the full current list
 * (IN_PROGRESS + READY) on connect and on every status change.
 *
 * The access token is passed as a query parameter because the browser
 * EventSource API cannot set an Authorization header.
 */
export function openOrderBoardStream(shopId: number): EventSource {
  const token = readAccessTokenFromStorage()
  const params = new URLSearchParams({ shopId: String(shopId) })
  if (token) {
    params.set('access_token', token)
  }
  return new EventSource(`${API_BASE_URL}/admin/orders/board/stream?${params.toString()}`)
}
