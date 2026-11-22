/**
 * Product photos are uploaded from the browser straight to Cloudinary with an unsigned preset,
 * then the resulting URL is saved on the product via the backend.
 *
 * Every coffee shop gets its own folder: `coffee-mvp/shop-<shopId>/products`.
 */

const CLOUD_NAME = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME ?? ''
const UPLOAD_PRESET = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET ?? ''

export const CLOUDINARY_ROOT_FOLDER = 'coffee-mvp'

export function isCloudinaryConfigured() {
  return CLOUD_NAME !== '' && UPLOAD_PRESET !== ''
}

export function shopProductsFolder(shopId: number) {
  return `${CLOUDINARY_ROOT_FOLDER}/shop-${shopId}/products`
}

type CloudinaryUploadResponse = {
  secure_url: string
  public_id: string
  error?: { message: string }
}

export async function uploadToCloudinary(file: File, options: { shopId: number; productId: number }) {
  if (!isCloudinaryConfigured()) {
    throw new Error('Cloudinary не настроен (VITE_CLOUDINARY_CLOUD_NAME / VITE_CLOUDINARY_UPLOAD_PRESET)')
  }

  const data = new FormData()
  data.append('file', file)
  data.append('upload_preset', UPLOAD_PRESET)
  data.append('folder', shopProductsFolder(options.shopId))
  // Unique id per upload: unsigned presets can't overwrite, and a fresh URL busts CDN caches.
  data.append('public_id', `product-${options.productId}-${Date.now()}`)
  data.append('tags', `coffee-mvp,shop-${options.shopId},product-${options.productId}`)

  const response = await fetch(`https://api.cloudinary.com/v1_1/${CLOUD_NAME}/image/upload`, {
    method: 'POST',
    body: data,
  })
  const body = (await response.json().catch(() => null)) as CloudinaryUploadResponse | null

  if (!response.ok || !body?.secure_url) {
    throw new Error(body?.error?.message ?? 'Не удалось загрузить фото в Cloudinary')
  }
  return body
}

/**
 * Adds on-the-fly resize/format transforms to a Cloudinary delivery URL; other URLs are returned as-is.
 */
export function cloudinaryThumb(url: string | undefined, width: number) {
  if (!url || !url.startsWith('https://res.cloudinary.com/') || !url.includes('/image/upload/')) return url
  return url.replace('/image/upload/', `/image/upload/c_fill,w_${width},h_${width},q_auto,f_auto/`)
}
