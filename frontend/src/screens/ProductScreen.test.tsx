// @vitest-environment jsdom

import { MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { ProductDto } from '../api/products'
import { cleanupDocument, clickElement, renderIntoDocument, waitFor } from '../testUtils/dom'
import { ProductScreen } from './ProductScreen'

const mockProductsApi = vi.hoisted(() => ({
  getProductById: vi.fn(),
}))

const mockCart = vi.hoisted(() => ({
  addItem: vi.fn(),
}))

vi.mock('../api/products', () => mockProductsApi)
vi.mock('../cart/CartContext', () => ({
  useCart: () => mockCart,
}))
vi.mock('../favorites/FavoritesContext', () => ({
  useFavorites: () => ({
    isFavorite: () => false,
    toggleFavorite: vi.fn(),
  }),
}))
vi.mock('../shop/ShopContext', () => ({
  useShop: () => ({
    shop: {
      id: 7,
      name: 'Mega Park',
    },
  }),
}))

function productFixture(overrides: Partial<ProductDto> = {}): ProductDto {
  return {
    id: 10,
    name: 'Iced latte',
    category: 'coffee',
    categoryNameRu: 'Coffee',
    basePrice: 2600,
    available: true,
    imagePath: null,
    description: null,
    availableToppings: [],
    ...overrides,
  }
}

function NextProductButton() {
  const navigate = useNavigate()

  return (
    <button type="button" onClick={() => navigate('/product/11')}>
      next product
    </button>
  )
}

describe('ProductScreen', () => {
  beforeEach(() => {
    mockCart.addItem.mockReset()
    mockProductsApi.getProductById.mockReset()
    mockProductsApi.getProductById.mockResolvedValue(productFixture())
  })

  afterEach(async () => {
    await cleanupDocument()
  })

  it('adds the product to the selected shop cart', async () => {
    const { container } = await renderIntoDocument(
      <MemoryRouter initialEntries={['/product/10']}>
        <Routes>
          <Route path="/product/:productId" element={<ProductScreen />} />
          <Route path="/cart" element={<span>cart</span>} />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(container.textContent).toContain('Iced latte')
    })

    const buttons = Array.from(container.querySelectorAll('button'))
    const addButton = buttons[buttons.length - 1]
    expect(addButton).not.toBeUndefined()
    await clickElement(addButton!)

    expect(mockCart.addItem).toHaveBeenCalledWith(
      expect.objectContaining({ id: 10, name: 'Iced latte' }),
      [],
      1,
      7,
    )
  })

  it('does not add unavailable products to the cart', async () => {
    mockProductsApi.getProductById.mockResolvedValue(productFixture({ available: false }))

    const { container } = await renderIntoDocument(
      <MemoryRouter initialEntries={['/product/10']}>
        <Routes>
          <Route path="/product/:productId" element={<ProductScreen />} />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(container.textContent).toContain('Iced latte')
    })

    const buttons = Array.from(container.querySelectorAll('button'))
    const addButton = buttons[buttons.length - 1] as HTMLButtonElement | undefined
    expect(addButton).not.toBeUndefined()
    expect(addButton!.disabled).toBe(true)

    await clickElement(addButton!)

    expect(mockCart.addItem).not.toHaveBeenCalled()
  })

  it('does not carry selected toppings across product route changes', async () => {
    mockProductsApi.getProductById.mockImplementation((id: number) =>
      Promise.resolve(
        id === 10
          ? productFixture({
              id: 10,
              name: 'Iced latte',
              availableToppings: [
                {
                  id: 1,
                  name: 'Vanilla',
                  type: 'SYRUP',
                  typeNameRu: 'Syrup',
                  price: 200,
                  incompatibleWithIds: [],
                },
              ],
            })
          : productFixture({
              id: 11,
              name: 'Mocha',
              availableToppings: [
                {
                  id: 2,
                  name: 'Caramel',
                  type: 'SYRUP',
                  typeNameRu: 'Syrup',
                  price: 250,
                  incompatibleWithIds: [],
                },
              ],
            }),
      ),
    )

    const { container } = await renderIntoDocument(
      <MemoryRouter initialEntries={['/product/10']}>
        <NextProductButton />
        <Routes>
          <Route path="/product/:productId" element={<ProductScreen />} />
          <Route path="/cart" element={<span>cart</span>} />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(container.textContent).toContain('Vanilla')
    })

    const vanillaButton = Array.from(container.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('Vanilla'),
    )
    expect(vanillaButton).not.toBeUndefined()
    await clickElement(vanillaButton!)

    const nextButton = Array.from(container.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('next product'),
    )
    expect(nextButton).not.toBeUndefined()
    await clickElement(nextButton!)

    await waitFor(() => {
      expect(container.textContent).toContain('Mocha')
    })

    const buttons = Array.from(container.querySelectorAll('button'))
    const addButton = buttons[buttons.length - 1]
    expect(addButton).not.toBeUndefined()
    await clickElement(addButton!)

    expect(mockCart.addItem).toHaveBeenCalledWith(expect.objectContaining({ id: 11, name: 'Mocha' }), [], 1, 7)
  })
})
