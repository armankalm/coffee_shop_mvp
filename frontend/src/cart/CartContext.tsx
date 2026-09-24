import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

import type { ProductDto } from '../api/products'

const STORAGE_KEY = 'drinkit.cart'

export type CartLine = {
  id: string
  shopId: number
  productId: number
  productName: string
  imagePath: string | null
  basePrice: number
  toppingIds: number[]
  toppingsLabel: string
  toppingsPrice: number
  quantity: number
}

type CartContextValue = {
  lines: CartLine[]
  addItem: (product: ProductDto, toppingIds: number[], quantity: number, shopId: number) => void
  /** Replaces the toppings of an existing line in place, keeping its quantity. */
  replaceItem: (lineId: string, product: ProductDto, toppingIds: number[]) => void
  updateQuantity: (lineId: string, quantity: number) => void
  removeItem: (lineId: string) => void
  clearShop: (shopId: number) => void
  clear: () => void
}

const CartContext = createContext<CartContextValue | null>(null)

function isPositiveInteger(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value > 0
}

function isNonNegativeNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0
}

function isStringOrNull(value: unknown): value is string | null {
  return typeof value === 'string' || value === null
}

function isCartLine(line: unknown): line is CartLine {
  if (typeof line !== 'object' || line === null) return false

  const candidate = line as Partial<CartLine>

  return (
    typeof candidate.id === 'string' &&
    candidate.id.length > 0 &&
    isPositiveInteger(candidate.shopId) &&
    isPositiveInteger(candidate.productId) &&
    typeof candidate.productName === 'string' &&
    candidate.productName.length > 0 &&
    isStringOrNull(candidate.imagePath) &&
    isNonNegativeNumber(candidate.basePrice) &&
    Array.isArray(candidate.toppingIds) &&
    candidate.toppingIds.every(isPositiveInteger) &&
    typeof candidate.toppingsLabel === 'string' &&
    isNonNegativeNumber(candidate.toppingsPrice) &&
    isPositiveInteger(candidate.quantity)
  )
}

function readStoredLines(): CartLine[] {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return []

  try {
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) return []

    return parsed.filter(isCartLine)
  } catch {
    return []
  }
}

function lineKey(shopId: number, productId: number, toppingIds: number[]) {
  return `${shopId}:${productId}:${[...toppingIds].sort((a, b) => a - b).join(',')}`
}

function buildLine(product: ProductDto, toppingIds: number[], quantity: number, shopId: number): CartLine {
  const requestedToppingIds = new Set(toppingIds)
  const toppings = product.availableToppings.filter((topping) => requestedToppingIds.has(topping.id))
  const validToppingIds = toppings.map((topping) => topping.id)

  return {
    id: lineKey(shopId, product.id, validToppingIds),
    shopId,
    productId: product.id,
    productName: product.name,
    imagePath: product.imagePath,
    basePrice: product.basePrice,
    toppingIds: validToppingIds,
    toppingsLabel: toppings.map((topping) => topping.name).join(', '),
    toppingsPrice: toppings.reduce((sum, topping) => sum + topping.price, 0),
    quantity,
  }
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [lines, setLines] = useState<CartLine[]>(() => readStoredLines())

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(lines))
  }, [lines])

  const value = useMemo<CartContextValue>(
    () => ({
      lines,
      addItem: (product, toppingIds, quantity, shopId) => {
        if (!product.available || !isPositiveInteger(quantity) || !isPositiveInteger(shopId)) return

        const newLine = buildLine(product, toppingIds, quantity, shopId)

        setLines((currentLines) => {
          const existing = currentLines.find((line) => line.id === newLine.id)

          return existing
            ? currentLines.map((line) =>
                line.id === newLine.id ? { ...line, quantity: line.quantity + quantity } : line,
              )
            : [...currentLines, newLine]
        })
      },
      replaceItem: (lineId, product, toppingIds) => {
        if (!product.available) return

        setLines((currentLines) => {
          const original = currentLines.find((line) => line.id === lineId)
          if (!original || original.productId !== product.id) return currentLines

          const edited = buildLine(product, toppingIds, original.quantity, original.shopId)
          if (edited.id === lineId) {
            return currentLines.map((line) => (line.id === lineId ? edited : line))
          }

          // The new toppings match another line: merge into it instead of keeping two identical lines.
          const duplicate = currentLines.find((line) => line.id === edited.id)
          if (duplicate) {
            return currentLines
              .filter((line) => line.id !== lineId)
              .map((line) => (line.id === edited.id ? { ...line, quantity: line.quantity + original.quantity } : line))
          }
          return currentLines.map((line) => (line.id === lineId ? edited : line))
        })
      },
      updateQuantity: (lineId, quantity) => {
        if (quantity <= 0) {
          setLines((currentLines) => currentLines.filter((line) => line.id !== lineId))
          return
        }
        setLines((currentLines) => currentLines.map((line) => (line.id === lineId ? { ...line, quantity } : line)))
      },
      removeItem: (lineId) => {
        setLines((currentLines) => currentLines.filter((line) => line.id !== lineId))
      },
      clearShop: (shopId) => {
        setLines((currentLines) => currentLines.filter((line) => line.shopId !== shopId))
      },
      clear: () => {
        setLines([])
      },
    }),
    [lines],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  const context = useContext(CartContext)
  if (!context) {
    throw new Error('useCart must be used within a CartProvider')
  }
  return context
}
