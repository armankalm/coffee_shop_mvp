import { apiGet, apiPost } from './client'
import type { ToppingDto } from './products'

export type OrderItemDto = {
  id: number
  productId: number
  productName: string
  toppings: ToppingDto[]
  quantity: number
  price: number
}

export type OrderDto = {
  id: number
  userId: number
  customerName: string
  shopId: number
  shopName: string
  /** Per-shop daily order number (null for legacy orders). */
  dailyNumber: number | null
  /** Local business date the daily number belongs to (ISO date). */
  orderDate: string | null
  status: string
  statusNameRu: string
  total: number
  createdAt: string
  items: OrderItemDto[]
}

export type CreateOrderItemRequest = {
  productId: number
  toppingIds: number[]
  quantity: number
}

export type CreateOrderRequest = {
  shopId: number
  customerName?: string
  items: CreateOrderItemRequest[]
}

export function createOrder(request: CreateOrderRequest) {
  return apiPost<OrderDto>('/orders', request, true)
}

export function getUserOrders() {
  return apiGet<OrderDto[]>('/orders')
}

export function getOrderById(orderId: number) {
  return apiGet<OrderDto>(`/orders/${orderId}`)
}

export function cancelOrder(orderId: number) {
  return apiPost<OrderDto>(`/orders/${orderId}/cancel`, undefined, true)
}
