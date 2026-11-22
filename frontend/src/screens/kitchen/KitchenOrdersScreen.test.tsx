// @vitest-environment jsdom

import { renderToStaticMarkup } from 'react-dom/server'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { OrderPosition, OrderPositionStatus } from '../../types'
import { cleanupDocument, clickElement, renderIntoDocument } from '../../testUtils/dom'
import { KitchenOrdersScreen } from './KitchenOrdersScreen'

const mockKitchenBoard = vi.hoisted(() => ({
  actionError: null as string | null,
  advancePosition: vi.fn(),
  advancePositions: vi.fn(),
  error: null as string | null,
  loading: false,
  pendingPositionIds: [] as string[],
  positions: [] as OrderPosition[],
  counts: { NEW: 0, IN_PROGRESS: 0, READY: 0, COMPLETED: 0 },
  positionsByStatus: vi.fn(),
}))

vi.mock('../../kitchen/KitchenBoardContext', () => ({
  useKitchenBoard: () => mockKitchenBoard,
}))

const positions: OrderPosition[] = [
  { id: 'pos-new', orderId: 101, orderNumber: '#101', title: 'Cortado', status: 'NEW', createdAt: '2026-07-19T08:00:00.000Z' },
  {
    id: 'pos-progress',
    orderId: 102,
    orderNumber: '#102',
    title: 'Pour over',
    status: 'IN_PROGRESS',
    createdAt: '2026-07-19T08:01:00.000Z',
  },
  { id: 'pos-ready', orderId: 103, orderNumber: '#103', title: 'Matcha', status: 'READY', createdAt: '2026-07-19T08:02:00.000Z' },
  { id: 'pos-ready-2', orderId: 103, orderNumber: '#103', title: 'Cookie', status: 'READY', createdAt: '2026-07-19T08:02:00.000Z' },
  { id: 'pos-sibling', orderId: 101, orderNumber: '#101', title: 'Croissant', status: 'READY', createdAt: '2026-07-19T08:00:00.000Z' },
]

function positionsByStatus(status: OrderPositionStatus) {
  return positions.filter((position) => position.status === status)
}

describe('KitchenOrdersScreen', () => {
  beforeEach(() => {
    mockKitchenBoard.actionError = null
    mockKitchenBoard.advancePosition.mockReset()
    mockKitchenBoard.advancePositions.mockReset()
    mockKitchenBoard.error = null
    mockKitchenBoard.loading = false
    mockKitchenBoard.pendingPositionIds = []
    mockKitchenBoard.positions = positions
    mockKitchenBoard.counts = { NEW: 1, IN_PROGRESS: 1, READY: 3, COMPLETED: 0 }
    mockKitchenBoard.positionsByStatus.mockReset()
    mockKitchenBoard.positionsByStatus.mockImplementation(positionsByStatus)
  })

  afterEach(async () => {
    await cleanupDocument()
  })

  it('renders all three kanban columns with counts, marking the routed one active', () => {
    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="IN_PROGRESS" />)

    for (const status of ['NEW', 'IN_PROGRESS', 'READY'] as const) {
      expect(mockKitchenBoard.positionsByStatus).toHaveBeenCalledWith(status)
      expect(markup).toContain(`data-column="${status}"`)
      expect(markup).toContain(`data-status-filter="${status}"`)
    }
    expect(markup).toContain('data-active-status="IN_PROGRESS"')
    expect(markup).toContain('Cortado')
    expect(markup).toContain('Pour over')
    expect(markup).toContain('Matcha')
  })

  it('labels each action with the next step', () => {
    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="NEW" />)

    expect(markup).toContain('Начать')
    expect(markup).toContain('Готово')
    expect(markup).toContain('Выдать заказ')
  })

  it('shows the other items of the same order on a card', () => {
    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="NEW" />)

    expect(markup).toContain('В заказе ещё')
    expect(markup).toContain('Croissant')
    expect(markup).toContain('(готов)')
  })

  it('renders loading state before the board', () => {
    mockKitchenBoard.loading = true

    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="NEW" />)

    expect(markup).toContain('data-status-filter="NEW"')
    expect(markup).toContain('Загружаем заказы')
    expect(markup).not.toContain('Очередь пуста')
    expect(mockKitchenBoard.positionsByStatus).not.toHaveBeenCalled()
  })

  it('renders error state before the board', () => {
    mockKitchenBoard.error = 'Load failed'

    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="NEW" />)

    expect(markup).toContain('role="alert"')
    expect(markup).toContain('Не удалось загрузить заказы')
    expect(markup).toContain('Load failed')
    expect(mockKitchenBoard.positionsByStatus).not.toHaveBeenCalled()
  })

  it('renders action errors without replacing loaded positions', () => {
    mockKitchenBoard.actionError = 'Advance failed'

    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="NEW" />)

    expect(markup).toContain('role="alert"')
    expect(markup).toContain('Не удалось обновить статус')
    expect(markup).toContain('Advance failed')
    expect(markup).toContain('Cortado')
  })

  it('disables cards with pending position updates', () => {
    mockKitchenBoard.pendingPositionIds = ['pos-new']

    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="NEW" />)

    expect(markup).toContain('aria-busy="true"')
    expect(markup).toContain('disabled=""')
  })

  it('wires the card action button to advancePosition', async () => {
    const { container } = await renderIntoDocument(<KitchenOrdersScreen statusFilter="NEW" />)
    const button = container.querySelector('[data-position-id="pos-new"] button')

    expect(button).not.toBeNull()
    await clickElement(button!)

    expect(mockKitchenBoard.advancePosition).toHaveBeenCalledWith('pos-new', 'NEW')
  })

  it('issues every ready item of an order at once', async () => {
    const { container } = await renderIntoDocument(<KitchenOrdersScreen statusFilter="READY" />)
    const issue = Array.from(container.querySelectorAll('[aria-label="Заказ #103"] button'))[0]

    expect(issue).not.toBeUndefined()
    await clickElement(issue!)

    expect(mockKitchenBoard.advancePositions).toHaveBeenCalledWith(['pos-ready', 'pos-ready-2'], 'READY')
  })
})
