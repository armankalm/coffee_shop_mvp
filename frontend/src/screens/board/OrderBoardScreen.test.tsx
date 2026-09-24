// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { BoardOrder } from '../../api/board'
import { cleanupDocument, renderIntoDocument } from '../../testUtils/dom'
import { OrderBoardScreen } from './OrderBoardScreen'

const mockBoard = vi.hoisted(() => ({
  orders: [] as BoardOrder[],
  connected: true,
  error: null as string | null,
}))

const mockParams = vi.hoisted(() => ({ shopId: '1' as string | undefined }))

vi.mock('../../board/useOrderBoard', () => ({
  useOrderBoard: () => mockBoard,
}))

vi.mock('react-router-dom', () => ({
  useParams: () => mockParams,
}))

const orders: BoardOrder[] = [
  { orderId: 341, dailyNumber: 1, orderNumber: '№1', customerName: 'Ментос', status: 'IN_PROGRESS' },
  { orderId: 345, dailyNumber: 2, orderNumber: '№2', customerName: 'Гость', status: 'IN_PROGRESS' },
  { orderId: 350, dailyNumber: 3, orderNumber: '№3', customerName: 'Новый', status: 'NEW' },
  { orderId: 400, dailyNumber: 4, orderNumber: '№4', customerName: 'Анна', status: 'READY' },
]

describe('OrderBoardScreen', () => {
  beforeEach(() => {
    mockBoard.orders = orders
    mockBoard.connected = true
    mockParams.shopId = '1'
  })

  afterEach(async () => {
    await cleanupDocument()
  })

  it('puts NEW and IN_PROGRESS in "В работе" and READY in "Готовы"', async () => {
    const { container } = await renderIntoDocument(<OrderBoardScreen />)

    const titles = [...container.querySelectorAll('h2')].map((node) => node.textContent)
    expect(titles).toEqual(['В работе', 'Готовы'])

    const columns = container.querySelectorAll('section')
    const [inProgressColumn, readyColumn] = columns
    // "В работе" = 2 IN_PROGRESS + 1 NEW; "Готовы" = 1 READY.
    expect(inProgressColumn?.querySelectorAll('li')).toHaveLength(3)
    expect(inProgressColumn?.textContent).toContain('Новый')
    expect(readyColumn?.querySelectorAll('li')).toHaveLength(1)
  })

  it('renders order number and customer name for each row', async () => {
    const { container } = await renderIntoDocument(<OrderBoardScreen />)

    // Board badges show the bare number, without the "№" prefix.
    const badge = container.querySelector('[class*="badge"]')
    expect(badge?.textContent).toBe('1')
    expect(container.textContent).toContain('Ментос')
    expect(container.textContent).toContain('Анна')
  })

  it('shows a hint when the shop id is missing', async () => {
    mockParams.shopId = undefined

    const { container } = await renderIntoDocument(<OrderBoardScreen />)

    expect(container.textContent).toContain('Не указана кофейня')
    expect(container.querySelector('h2')).toBeNull()
  })

  it('marks READY order badges as green (ready) and others not', async () => {
    const { container } = await renderIntoDocument(<OrderBoardScreen />)

    const [inProgressColumn, readyColumn] = container.querySelectorAll('section')

    // "Готовы" badge carries the ready modifier; "В работе" badges do not.
    expect(readyColumn?.querySelector('[class*="badgeReady"]')).not.toBeNull()
    expect(inProgressColumn?.querySelector('[class*="badgeReady"]')).toBeNull()
  })

  it('paginates each column independently by its own item count', async () => {
    const make = (start: number, count: number, status: 'IN_PROGRESS' | 'READY') =>
      Array.from({ length: count }, (_, index) => ({
        orderId: start + index,
        dailyNumber: start + index,
        orderNumber: `№${start + index}`,
        customerName: `Гость ${start + index}`,
        status,
      }))

    // "В работе": 14 items -> 3 pages; "Готовы": 8 items -> 2 pages.
    mockBoard.orders = [...make(1, 14, 'IN_PROGRESS'), ...make(100, 8, 'READY')]

    const { container } = await renderIntoDocument(<OrderBoardScreen />)

    const [inProgressColumn, readyColumn] = container.querySelectorAll('section')

    // Each column shows 6 per page.
    expect(inProgressColumn?.querySelectorAll('li')).toHaveLength(6)
    expect(readyColumn?.querySelectorAll('li')).toHaveLength(6)

    // Each column has its own progress bars: 3 vs 2.
    expect(inProgressColumn?.querySelectorAll('[class*="progressTrack"]').length).toBe(3)
    expect(readyColumn?.querySelectorAll('[class*="progressTrack"]').length).toBe(2)
  })
})
