// @vitest-environment jsdom

import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { OrderDto } from '../api/orders'
import { cleanupDocument, renderIntoDocument, waitFor } from '../testUtils/dom'
import { ProfileScreen } from './ProfileScreen'

const mockApi = vi.hoisted(() => ({
  getCurrentUser: vi.fn(),
  getUserOrders: vi.fn(),
  getProductById: vi.fn(),
}))

vi.mock('../api/user', () => ({ getCurrentUser: mockApi.getCurrentUser }))
vi.mock('../api/orders', () => ({ getUserOrders: mockApi.getUserOrders }))
vi.mock('../api/products', () => ({ getProductById: mockApi.getProductById }))
const mockAuth = vi.hoisted(() => ({ session: null as { role: string } | null }))

vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ logout: vi.fn(), session: mockAuth.session }) }))
vi.mock('../cart/CartContext', () => ({ useCart: () => ({ addItem: vi.fn() }) }))
vi.mock('../shop/ShopContext', () => ({ useShop: () => ({ shop: null }) }))

function order(id: number, status: string, statusNameRu: string): OrderDto {
  return {
    id,
    userId: 1,
    customerName: '',
    shopId: 1,
    shopName: 'Центр',
    dailyNumber: id + 100,
    orderDate: null,
    status,
    statusNameRu,
    total: 1200,
    createdAt: '2026-09-24T10:00:00Z',
    items: [{ id: id * 10, productId: 2, productName: 'Капучино', toppings: [], quantity: 1, price: 1200 }],
  }
}

describe('ProfileScreen', () => {
  beforeEach(() => {
    mockApi.getCurrentUser.mockResolvedValue({ id: 1, email: 'guest@example.com', name: null, phone: null })
    mockApi.getProductById.mockResolvedValue({ imagePath: null })
  })

  afterEach(async () => {
    mockAuth.session = null
    vi.clearAllMocks()
    await cleanupDocument()
  })

  it('lists unfinished orders with a status bar and keeps finished ones in the history', async () => {
    mockApi.getUserOrders.mockResolvedValue([order(1, 'IN_PROGRESS', 'В работе'), order(2, 'COMPLETED', 'Завершён')])

    const { container } = await renderIntoDocument(
      <MemoryRouter>
        <ProfileScreen />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(container.textContent).toContain('Текущие заказы')
    })

    const active = container.querySelector('[aria-labelledby="active-orders-title"]')!
    expect(active.textContent).toContain('Заказ №101')
    expect(active.querySelector('[aria-current="step"]')?.textContent).toBe('Готовится')

    const history = container.querySelector('[aria-labelledby="orders-title"]')!
    expect(history.querySelector('a[href="/order/2"]')).not.toBeNull()
    expect(history.querySelector('a[href="/order/1"]')).toBeNull()
  })

  it('offers staff a way back to their workspace, but not customers', async () => {
    mockApi.getUserOrders.mockResolvedValue([])

    mockAuth.session = { role: 'BARISTA' }
    const staff = await renderIntoDocument(
      <MemoryRouter>
        <ProfileScreen />
      </MemoryRouter>,
    )
    await waitFor(() => {
      expect(staff.container.querySelector('a[href="/staff"]')).not.toBeNull()
    })
    await cleanupDocument()

    mockAuth.session = { role: 'USER' }
    const customer = await renderIntoDocument(
      <MemoryRouter>
        <ProfileScreen />
      </MemoryRouter>,
    )
    await waitFor(() => {
      expect(customer.container.textContent).toContain('История заказов')
    })
    expect(customer.container.querySelector('a[href="/staff"]')).toBeNull()
  })

  it('hides the block when nothing is in progress', async () => {
    mockApi.getUserOrders.mockResolvedValue([order(2, 'COMPLETED', 'Завершён')])

    const { container } = await renderIntoDocument(
      <MemoryRouter>
        <ProfileScreen />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(container.textContent).toContain('История заказов')
    })
    expect(container.textContent).not.toContain('Текущие заказы')
  })
})
