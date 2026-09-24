// @vitest-environment jsdom

import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { CartProvider } from '../cart/CartContext'
import { ShopProvider } from '../shop/ShopContext'
import { cleanupDocument, clickElement, renderIntoDocument, waitFor } from '../testUtils/dom'
import { CartScreen } from './CartScreen'

const mockOrdersApi = vi.hoisted(() => ({
  createOrder: vi.fn(),
}))

vi.mock('../api/orders', () => mockOrdersApi)

function readStoredCart() {
  return JSON.parse(localStorage.getItem('drinkit.cart') ?? '[]') as Array<{ shopId: number; productId: number }>
}

describe('CartScreen', () => {
  beforeEach(() => {
    localStorage.clear()
    mockOrdersApi.createOrder.mockReset()
    mockOrdersApi.createOrder.mockResolvedValue({ id: 500 })
    localStorage.setItem(
      'drinkit.shop',
      JSON.stringify({
        id: 1,
        name: 'Mega Park',
        city: { id: 1, name: 'Almaty', region: 'Almaty' },
        address: 'Abylai 1',
        status: 'ACTIVE',
        statusNameRu: 'Open',
      }),
    )
    localStorage.setItem(
      'drinkit.cart',
      JSON.stringify([
        {
          id: '1:10:',
          shopId: 1,
          productId: 10,
          productName: 'Current shop latte',
          imagePath: null,
          basePrice: 2600,
          toppingIds: [],
          toppingsLabel: '',
          toppingsPrice: 0,
          quantity: 1,
        },
        {
          id: '2:11:',
          shopId: 2,
          productId: 11,
          productName: 'Other shop flat white',
          imagePath: null,
          basePrice: 1800,
          toppingIds: [],
          toppingsLabel: '',
          toppingsPrice: 0,
          quantity: 1,
        },
      ]),
    )
  })

  afterEach(async () => {
    await cleanupDocument()
  })

  it('renders and submits only the selected shop cart lines', async () => {
    const { container } = await renderIntoDocument(
      <MemoryRouter>
        <ShopProvider>
          <CartProvider>
            <CartScreen />
          </CartProvider>
        </ShopProvider>
      </MemoryRouter>,
    )

    expect(container.textContent).toContain('Current shop latte')
    expect(container.textContent).not.toContain('Other shop flat white')

    const payButton = Array.from(container.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('Оформить заказ'),
    )
    expect(payButton).not.toBeUndefined()
    await clickElement(payButton!)

    await waitFor(() => {
      expect(mockOrdersApi.createOrder).toHaveBeenCalledWith({
        shopId: 1,
        items: [{ productId: 10, toppingIds: [], quantity: 1 }],
      })
    })

    await waitFor(() => {
      expect(readStoredCart()).toEqual([
        expect.objectContaining({
          shopId: 2,
          productId: 11,
        }),
      ])
    })
  })

  it('clears only the selected shop cart lines from the trash button', async () => {
    const { container } = await renderIntoDocument(
      <MemoryRouter>
        <ShopProvider>
          <CartProvider>
            <CartScreen />
          </CartProvider>
        </ShopProvider>
      </MemoryRouter>,
    )

    const trashButton = container.querySelector('header button')
    expect(trashButton).not.toBeNull()
    await clickElement(trashButton!)

    await waitFor(() => {
      expect(readStoredCart()).toEqual([
        expect.objectContaining({
          shopId: 2,
          productId: 11,
        }),
      ])
    })
  })
})
