import { describe, expect, it } from 'vitest'

import {
  cartItems,
  categories,
  crossSellItems,
  modifiers,
  orderHistory,
  paymentMethods,
  pickupPoints,
  products,
  userProfile,
} from './mocks'

describe('mock data', () => {
  it('contains the core app entities required by the routed screens', () => {
    expect(pickupPoints.length).toBeGreaterThanOrEqual(4)
    expect(userProfile.name).toBeTruthy()
    expect(orderHistory.length).toBeGreaterThan(0)
    expect(categories.length).toBeGreaterThan(0)
    expect(products.length).toBeGreaterThan(0)
    expect(modifiers.length).toBeGreaterThan(0)
    expect(crossSellItems.length).toBeGreaterThan(0)
    expect(paymentMethods.some((method) => method.selected)).toBe(true)
  })

  it('keeps product, cart, and order references valid', () => {
    const categoryIds = new Set(categories.map((category) => category.id))
    const productIds = new Set(products.map((product) => product.id))
    const pointIds = new Set(pickupPoints.map((point) => point.id))
    const modifierIds = new Set(modifiers.map((modifier) => modifier.id))
    const crossSellIds = new Set(crossSellItems.map((item) => item.id))
    const sellableIds = new Set([...productIds, ...crossSellIds])

    expect(products.every((product) => categoryIds.has(product.categoryId))).toBe(true)
    expect(products.every((product) => product.modifierIds.every((modifierId) => modifierIds.has(modifierId)))).toBe(
      true,
    )
    expect(products.every((product) => product.crossSellIds.every((itemId) => crossSellIds.has(itemId)))).toBe(true)
    expect(cartItems.every((item) => productIds.has(item.productId))).toBe(true)
    expect(orderHistory.every((order) => pointIds.has(order.pointId))).toBe(true)
    expect(orderHistory.every((order) => order.items.every((item) => sellableIds.has(item.productId)))).toBe(true)
  })
})
