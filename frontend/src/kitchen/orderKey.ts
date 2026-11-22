import type { OrderPosition } from '../types'

/** «27» → «№27»; numbers that already carry a prefix («#104», «№104») are kept. */
export function displayOrderNumber(orderNumber: string) {
  return /^\d+$/.test(orderNumber) ? `№${orderNumber}` : orderNumber
}

/** Identifies the order a board position belongs to (orderId, or orderNumber for older payloads). */
export function orderKey(position: OrderPosition) {
  return position.orderId != null ? `id:${position.orderId}` : `no:${position.orderNumber}`
}
