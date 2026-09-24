// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { ProductDto } from '../../api/products'
import type { CoffeeShopDto } from '../../api/shops'
import { cleanupDocument, clickElement, renderIntoDocument, waitFor } from '../../testUtils/dom'
import { PosOrderScreen } from './PosOrderScreen'

const mockStaff = vi.hoisted(() => ({
  shops: [] as CoffeeShopDto[],
  selectedShopId: 5 as number | null,
  selectShop: vi.fn(),
  loading: false,
  error: null as string | null,
}))

const mockApi = vi.hoisted(() => ({
  getProducts: vi.fn(),
  createOrder: vi.fn(),
}))

vi.mock('../../staff/useStaffShops', () => ({
  useStaffShops: () => mockStaff,
}))

vi.mock('../../api/products', () => ({
  getProducts: mockApi.getProducts,
}))

vi.mock('../../api/orders', () => ({
  createOrder: mockApi.createOrder,
}))

vi.mock('react-router-dom', () => ({
  Link: ({ children }: { children: unknown }) => <span>{children as never}</span>,
}))

const products: ProductDto[] = [
  {
    id: 11,
    name: 'Латте',
    category: 'coffee',
    categoryNameRu: 'Кофе',
    basePrice: 1500,
    available: true,
    imagePath: null,
    description: null,
    availableToppings: [],
  },
  {
    id: 22,
    name: 'Круассан',
    category: 'bakery',
    categoryNameRu: 'Выпечка',
    basePrice: 900,
    available: true,
    imagePath: null,
    description: null,
    availableToppings: [],
  },
]

describe('PosOrderScreen', () => {
  beforeEach(() => {
    mockStaff.shops = [
      {
        id: 5,
        name: 'Mega Park',
        address: 'ул.',
        status: 'OPEN',
        statusNameRu: 'Открыто',
        city: { id: 1, name: 'Алматы', region: 'Алматы' },
      },
    ]
    mockStaff.selectedShopId = 5
    mockApi.getProducts.mockResolvedValue(products)
    mockApi.createOrder.mockResolvedValue({ id: 777, dailyNumber: 7 })
  })

  afterEach(async () => {
    await cleanupDocument()
    vi.clearAllMocks()
  })

  it('creates an order with the guest name and selected items', async () => {
    const { container } = await renderIntoDocument(<PosOrderScreen />)

    await waitFor(() => {
      if (!container.textContent?.includes('Латте')) throw new Error('menu not loaded')
    })

    const increase = container.querySelector('button[aria-label="Добавить Латте"]')
    if (!increase) throw new Error('increase button missing')
    await clickElement(increase)
    await clickElement(increase)

    const submit = [...container.querySelectorAll('button')].find((node) =>
      node.textContent?.includes('Создать заказ'),
    )
    if (!submit) throw new Error('submit button missing')
    await clickElement(submit)

    await waitFor(() => {
      if (!mockApi.createOrder.mock.calls.length) throw new Error('not submitted')
    })

    expect(mockApi.createOrder).toHaveBeenCalledWith({
      shopId: 5,
      items: [{ productId: 11, toppingIds: [], quantity: 2 }],
    })
  })

  it('groups the menu by category and filters when a tab is chosen', async () => {
    const { container } = await renderIntoDocument(<PosOrderScreen />)

    await waitFor(() => {
      if (!container.textContent?.includes('Круассан')) throw new Error('menu not loaded')
    })

    // Both category headings shown initially.
    const headings = [...container.querySelectorAll('h2')].map((node) => node.textContent)
    expect(headings).toContain('Кофе')
    expect(headings).toContain('Выпечка')

    // Choosing "Выпечка" hides the coffee group.
    const bakeryTab = [...container.querySelectorAll('[role="tab"]')].find(
      (node) => node.textContent === 'Выпечка',
    )
    if (!bakeryTab) throw new Error('bakery tab missing')
    await clickElement(bakeryTab)

    expect(container.textContent).toContain('Круассан')
    expect(container.textContent).not.toContain('Латте')
  })
})
