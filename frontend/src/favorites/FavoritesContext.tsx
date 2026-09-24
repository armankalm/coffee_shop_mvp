import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

import { addFavorite, getFavorites, removeFavorite } from '../api/favorites'
import type { ProductDto } from '../api/products'
import { useAuth } from '../auth/AuthContext'

type FavoritesContextValue = {
  favoriteProducts: ProductDto[]
  favoriteProductIds: number[]
  isFavorite: (productId: number) => boolean
  toggleFavorite: (product: ProductDto) => void
}

type FavoritesState = {
  sessionKey: string | null
  products: ProductDto[]
}

const FavoritesContext = createContext<FavoritesContextValue | null>(null)
const emptyFavoriteProducts: ProductDto[] = []

export function FavoritesProvider({ children }: { children: ReactNode }) {
  const { session } = useAuth()
  const sessionKey = session?.accessToken ?? null
  const [favoritesState, setFavoritesState] = useState<FavoritesState>({ sessionKey: null, products: [] })

  useEffect(() => {
    if (!sessionKey) {
      return
    }

    let cancelled = false

    getFavorites()
      .then((favorites) => {
        if (!cancelled) {
          setFavoritesState({ sessionKey, products: favorites.map((favorite) => favorite.product) })
        }
      })
      .catch(() => {
        if (!cancelled) setFavoritesState({ sessionKey, products: [] })
      })

    return () => {
      cancelled = true
    }
  }, [sessionKey])

  const visibleFavoriteProducts =
    sessionKey && favoritesState.sessionKey === sessionKey ? favoritesState.products : emptyFavoriteProducts

  const toggleFavorite = useCallback(
    (product: ProductDto) => {
      if (!sessionKey) return

      const isCurrentlyFavorite = visibleFavoriteProducts.some((entry) => entry.id === product.id)

      if (isCurrentlyFavorite) {
        setFavoritesState((current) => {
          const products = current.sessionKey === sessionKey ? current.products : visibleFavoriteProducts
          return { sessionKey, products: products.filter((entry) => entry.id !== product.id) }
        })
        removeFavorite(product.id).catch(() => {
          setFavoritesState((current) =>
            current.sessionKey === sessionKey
              ? {
                  sessionKey,
                  products: current.products.some((entry) => entry.id === product.id)
                    ? current.products
                    : [...current.products, product],
                }
              : current,
          )
        })
        return
      }

      setFavoritesState((current) => {
        const products = current.sessionKey === sessionKey ? current.products : visibleFavoriteProducts
        return {
          sessionKey,
          products: products.some((entry) => entry.id === product.id) ? products : [...products, product],
        }
      })
      addFavorite(product.id).catch(() => {
        setFavoritesState((current) =>
          current.sessionKey === sessionKey
            ? { sessionKey, products: current.products.filter((entry) => entry.id !== product.id) }
            : current,
        )
      })
    },
    [sessionKey, visibleFavoriteProducts],
  )

  const favoriteProductIds = useMemo(
    () => visibleFavoriteProducts.map((product) => product.id),
    [visibleFavoriteProducts],
  )

  const value = useMemo<FavoritesContextValue>(
    () => ({
      favoriteProducts: visibleFavoriteProducts,
      favoriteProductIds,
      isFavorite: (productId) => favoriteProductIds.includes(productId),
      toggleFavorite,
    }),
    [visibleFavoriteProducts, favoriteProductIds, toggleFavorite],
  )

  return <FavoritesContext.Provider value={value}>{children}</FavoritesContext.Provider>
}

export function useFavorites() {
  const context = useContext(FavoritesContext)
  if (!context) {
    throw new Error('useFavorites must be used within a FavoritesProvider')
  }
  return context
}
