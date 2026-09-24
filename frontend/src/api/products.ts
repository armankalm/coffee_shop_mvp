import { apiGet } from './client'

export type ToppingDto = {
  id: number
  name: string
  type: string
  typeNameRu: string
  price: number
  incompatibleWithIds: number[]
}

export type ProductDto = {
  id: number
  name: string
  category: string
  categoryNameRu: string
  basePrice: number
  available: boolean
  imagePath: string | null
  description: string | null
  availableToppings: ToppingDto[]
}

export function getProducts(shopId: number, category?: string) {
  const query = new URLSearchParams({ shopId: String(shopId) })
  if (category) query.set('category', category)
  return apiGet<ProductDto[]>(`/products?${query.toString()}`)
}

export function getProductById(productId: number) {
  return apiGet<ProductDto>(`/products/${productId}`)
}
