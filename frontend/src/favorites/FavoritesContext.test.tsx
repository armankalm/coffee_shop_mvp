// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { ProductDto } from '../api/products'
import { AuthProvider, useAuth } from '../auth/AuthContext'
import { cleanupDocument, clickElement, renderIntoDocument, waitFor } from '../testUtils/dom'
import { FavoritesProvider, useFavorites } from './FavoritesContext'

const mockFavoritesApi = vi.hoisted(() => ({
  addFavorite: vi.fn(),
  getFavorites: vi.fn(),
  removeFavorite: vi.fn(),
}))

vi.mock('../api/favorites', () => mockFavoritesApi)

function productFixture(id: number, name: string): ProductDto {
  return {
    id,
    name,
    category: 'coffee',
    categoryNameRu: 'Coffee',
    basePrice: 2600,
    available: true,
    imagePath: null,
    description: null,
    availableToppings: [],
  }
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, reject, resolve }
}

function authResponse(accessToken: string) {
  return {
    accessToken,
    refreshToken: `${accessToken}-refresh`,
    email: `${accessToken}@example.com`,
    role: 'USER',
  }
}

function FavoritesProbe() {
  const { login, logout } = useAuth()
  const { favoriteProducts } = useFavorites()

  return (
    <div>
      <output>{favoriteProducts.map((product) => product.name).join('|') || 'empty'}</output>
      <button type="button" onClick={() => login(authResponse('first-user'))}>
        login first
      </button>
      <button type="button" onClick={() => login(authResponse('second-user'))}>
        login second
      </button>
      <button type="button" onClick={logout}>
        logout
      </button>
    </div>
  )
}

describe('FavoritesProvider', () => {
  beforeEach(() => {
    localStorage.clear()
    mockFavoritesApi.addFavorite.mockReset()
    mockFavoritesApi.getFavorites.mockReset()
    mockFavoritesApi.removeFavorite.mockReset()
  })

  afterEach(async () => {
    await cleanupDocument()
  })

  it('hides previous user favorites while the next session is loading', async () => {
    const secondFavorites = deferred<Array<{ id: number; product: ProductDto }>>()

    mockFavoritesApi.getFavorites
      .mockResolvedValueOnce([{ id: 1, product: productFixture(10, 'First user latte') }])
      .mockReturnValueOnce(secondFavorites.promise)

    const { container } = await renderIntoDocument(
      <AuthProvider>
        <FavoritesProvider>
          <FavoritesProbe />
        </FavoritesProvider>
      </AuthProvider>,
    )

    const buttons = container.querySelectorAll('button')

    await clickElement(buttons[0]!)

    await waitFor(() => {
      expect(container.textContent).toContain('First user latte')
    })

    await clickElement(buttons[2]!)

    await waitFor(() => {
      expect(container.textContent).toContain('empty')
      expect(container.textContent).not.toContain('First user latte')
    })

    await clickElement(buttons[1]!)

    expect(container.textContent).not.toContain('First user latte')

    secondFavorites.resolve([{ id: 2, product: productFixture(11, 'Second user mocha') }])

    await waitFor(() => {
      expect(container.textContent).toContain('Second user mocha')
      expect(container.textContent).not.toContain('First user latte')
    })
  })
})
