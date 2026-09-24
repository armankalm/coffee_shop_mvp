import { useCallback, useEffect, useMemo, useState } from 'react'

import type { CoffeeShopDto } from '../api/shops'
import { getCurrentUser } from '../api/user'

const SELECTED_SHOP_KEY = 'drinkit.staff.shopId'

export type StaffShopsState = {
  shops: CoffeeShopDto[]
  selectedShopId: number | null
  selectShop: (shopId: number) => void
  loading: boolean
  error: string | null
}

function readStoredShopId(): number | null {
  const raw = localStorage.getItem(SELECTED_SHOP_KEY)
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isFinite(parsed) ? parsed : null
}

/**
 * Loads the coffee shops the current staff member is assigned to and tracks
 * which one is active on the staff home / board. The selection is persisted so
 * a wall-mounted board keeps its shop across reloads.
 */
export function useStaffShops(): StaffShopsState {
  const [shops, setShops] = useState<CoffeeShopDto[]>([])
  const [selectedShopId, setSelectedShopId] = useState<number | null>(() => readStoredShopId())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    getCurrentUser()
      .then((user) => {
        if (cancelled) return
        const assigned = user.assignedShops ?? []
        setShops(assigned)
        setSelectedShopId((current) => {
          if (current != null && assigned.some((shop) => shop.id === current)) {
            return current
          }
          return assigned[0]?.id ?? null
        })
      })
      .catch((loadError: unknown) => {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить кофейни')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const selectShop = useCallback((shopId: number) => {
    localStorage.setItem(SELECTED_SHOP_KEY, String(shopId))
    setSelectedShopId(shopId)
  }, [])

  return useMemo(
    () => ({ shops, selectedShopId, selectShop, loading, error }),
    [shops, selectedShopId, selectShop, loading, error],
  )
}
