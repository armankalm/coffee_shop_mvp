import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

import type { CoffeeShopDto } from '../api/shops'

const STORAGE_KEY = 'drinkit.shop'

type ShopContextValue = {
  shop: CoffeeShopDto | null
  selectShop: (shop: CoffeeShopDto) => void
  clearShop: () => void
}

const ShopContext = createContext<ShopContextValue | null>(null)

function readStoredShop(): CoffeeShopDto | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as CoffeeShopDto
  } catch {
    return null
  }
}

export function ShopProvider({ children }: { children: ReactNode }) {
  const [shop, setShop] = useState<CoffeeShopDto | null>(() => readStoredShop())

  const value = useMemo<ShopContextValue>(
    () => ({
      shop,
      selectShop: (nextShop) => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(nextShop))
        setShop(nextShop)
      },
      clearShop: () => {
        localStorage.removeItem(STORAGE_KEY)
        setShop(null)
      },
    }),
    [shop],
  )

  return <ShopContext.Provider value={value}>{children}</ShopContext.Provider>
}

export function useShop() {
  const context = useContext(ShopContext)
  if (!context) {
    throw new Error('useShop must be used within a ShopProvider')
  }
  return context
}
