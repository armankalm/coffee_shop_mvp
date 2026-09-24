// @vitest-environment jsdom

import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import App from './App'
import { ApiError } from './api/client'
import { AuthProvider } from './auth/AuthContext'
import { CartProvider } from './cart/CartContext'
import { FavoritesProvider } from './favorites/FavoritesContext'
import { ShopProvider } from './shop/ShopContext'
import { cleanupDocument, renderIntoDocument, waitFor } from './testUtils/dom'

const mockProductsApi = vi.hoisted(() => ({
  getProducts: vi.fn(),
}))

const mockFavoritesApi = vi.hoisted(() => ({
  addFavorite: vi.fn(),
  getFavorites: vi.fn(),
  removeFavorite: vi.fn(),
}))

vi.mock('./api/products', () => mockProductsApi)
vi.mock('./api/favorites', () => mockFavoritesApi)

const testShop = {
  id: 1,
  name: 'Mega Park',
  city: { id: 1, name: 'Almaty', region: 'Almaty' },
  address: 'Abylai 1',
  status: 'ACTIVE',
  statusNameRu: 'Open',
}

function storeSession() {
  localStorage.setItem(
    'drinkit.auth',
    JSON.stringify({
      accessToken: 'test-access',
      refreshToken: 'test-refresh',
      email: 'test@example.com',
      role: 'USER',
    }),
  )
}

function storeShop() {
  localStorage.setItem('drinkit.shop', JSON.stringify(testShop))
}

function renderRoute(route: string) {
  return renderIntoDocument(
    <MemoryRouter initialEntries={[route]}>
      <AuthProvider>
        <ShopProvider>
          <FavoritesProvider>
            <CartProvider>
              <App />
            </CartProvider>
          </FavoritesProvider>
        </ShopProvider>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('App async routes', () => {
  beforeEach(() => {
    localStorage.clear()
    storeSession()
    storeShop()
    mockFavoritesApi.addFavorite.mockReset()
    mockFavoritesApi.getFavorites.mockReset()
    mockFavoritesApi.removeFavorite.mockReset()
    mockProductsApi.getProducts.mockReset()
    mockFavoritesApi.getFavorites.mockResolvedValue([])
  })

  afterEach(async () => {
    await cleanupDocument()
  })

  it('renders catalog products after the API resolves', async () => {
    mockProductsApi.getProducts.mockResolvedValue([
      {
        id: 10,
        name: 'Iced latte',
        category: 'coffee',
        categoryNameRu: 'Coffee',
        basePrice: 2600,
        available: true,
        imagePath: null,
        description: null,
        availableToppings: [],
      },
      {
        id: 11,
        name: 'Sold out raf',
        category: 'coffee',
        categoryNameRu: 'Coffee',
        basePrice: 3000,
        available: false,
        imagePath: null,
        description: null,
        availableToppings: [],
      },
    ])

    const { container } = await renderRoute('/catalog')

    await waitFor(() => {
      expect(container.textContent).toContain('Iced latte')
      expect(container.textContent).toContain('Coffee')
      expect(container.textContent).not.toContain('Sold out raf')
    })
  })

  it('renders catalog API errors after the API rejects', async () => {
    mockProductsApi.getProducts.mockRejectedValue(new ApiError(503, 'Menu unavailable'))

    const { container } = await renderRoute('/catalog')

    await waitFor(() => {
      expect(container.textContent).toContain('Menu unavailable')
    })
  })
})
