import { apiDelete, apiGet, apiPost, apiPut, apiUpload } from './client'
import type { ProductDto, ToppingDto } from './products'

export type ProductCategoryDto = {
  id: number
  code: string
  nameRu: string
  nameEn: string
  icon: string | null
}

export type ProductInput = {
  shopId: number
  name: string
  categoryCode: string
  basePrice: number
  available: boolean
  description: string | null
  availableToppingIds: number[]
}

/** All products of a shop for staff, including unavailable ones (soft-deleted are excluded). */
export function getShopProducts(shopId: number) {
  return apiGet<ProductDto[]>(`/admin/products?shopId=${shopId}`)
}

export function createProduct(input: ProductInput) {
  return apiPost<ProductDto>('/admin/products', input, true)
}

export function updateProduct(productId: number, input: ProductInput) {
  return apiPut<ProductDto>(`/admin/products/${productId}`, input)
}

export function deleteProduct(productId: number) {
  return apiDelete<void>(`/admin/products/${productId}`)
}

export function uploadProductImage(productId: number, file: File) {
  const data = new FormData()
  data.append('file', file)
  return apiUpload<ProductDto>(`/products/${productId}/image`, data)
}

export function getProductCategories() {
  return apiGet<ProductCategoryDto[]>('/admin/reference/product-categories')
}

export async function getAllToppings() {
  const grouped = await apiGet<Record<string, ToppingDto[]>>('/toppings')
  return Object.values(grouped).flat()
}
