import { apiDelete, apiGet, apiPost } from './client'
import type { ProductDto } from './products'

export type FavoriteProductDto = {
  id: number
  product: ProductDto
}

export function getFavorites() {
  return apiGet<FavoriteProductDto[]>('/favorite-products')
}

export function addFavorite(productId: number) {
  return apiPost<FavoriteProductDto>(`/favorite-products/${productId}`, undefined, true)
}

export function removeFavorite(productId: number) {
  return apiDelete<void>(`/favorite-products/${productId}`)
}
