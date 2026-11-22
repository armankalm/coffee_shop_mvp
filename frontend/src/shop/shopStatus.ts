import type { CoffeeShopDto } from '../api/shops'

export function isShopOpen(shop: CoffeeShopDto) {
  return shop.status === 'OPEN' || shop.status === 'ACTIVE'
}
