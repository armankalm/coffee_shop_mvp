import { apiDelete, apiGet, apiPost, apiPut, apiUpload } from './client'
import { isCloudinaryConfigured, uploadToCloudinary } from './cloudinary'
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

/**
 * Uploads the photo to Cloudinary into the shop's own folder and saves its URL on the product.
 * Falls back to the backend upload (local disk / Supabase) when Cloudinary isn't configured.
 */
export async function uploadProductImage(productId: number, shopId: number, file: File) {
  if (!isCloudinaryConfigured()) {
    const data = new FormData()
    data.append('file', file)
    return apiUpload<ProductDto>(`/products/${productId}/image`, data)
  }
  const uploaded = await uploadToCloudinary(file, { shopId, productId })
  return apiPut<ProductDto>(`/products/${productId}/image-url`, { imageUrl: uploaded.secure_url })
}

export function getProductCategories() {
  return apiGet<ProductCategoryDto[]>('/admin/reference/product-categories')
}

export async function getAllToppings() {
  const grouped = await apiGet<Record<string, ToppingDto[]>>('/toppings')
  return Object.values(grouped).flat()
}
