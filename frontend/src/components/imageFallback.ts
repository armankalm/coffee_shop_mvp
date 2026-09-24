import type { SyntheticEvent } from 'react'

import heroFallback from '../assets/hero.png'

/**
 * Swaps a broken product image (e.g. a file lost from the server disk) for the
 * default picture instead of showing the browser's broken-image alt text.
 */
export function showFallbackImage(event: SyntheticEvent<HTMLImageElement>) {
  const image = event.currentTarget
  if (image.dataset.fallback) return
  image.dataset.fallback = 'true'
  image.src = heroFallback
}
