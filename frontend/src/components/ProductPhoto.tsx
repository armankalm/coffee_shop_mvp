import { cloudinaryThumb } from '../api/cloudinary'
import { resolveAssetUrl } from '../api/client'
import heroFallback from '../assets/hero.png'
import { showFallbackImage } from './imageFallback'

export type ProductPhotoProps = {
  imagePath: string | null | undefined
  alt?: string
  className?: string | undefined
  /** Rendered width in CSS px; Cloudinary URLs are resized to 2× of it. Omit for the original. */
  width?: number
}

/** Product image with Cloudinary resizing and a fallback picture for missing/broken files. */
export function ProductPhoto({ imagePath, alt = '', className, width }: ProductPhotoProps) {
  const url = resolveAssetUrl(imagePath)
  const src = (width ? cloudinaryThumb(url, width * 2) : url) ?? heroFallback

  return (
    <span className={className}>
      <img src={src} alt={alt} draggable={false} loading="lazy" onError={showFallbackImage} />
    </span>
  )
}
