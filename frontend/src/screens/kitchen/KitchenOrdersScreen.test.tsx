// @vitest-environment jsdom

import { renderToStaticMarkup } from 'react-dom/server'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { OrderPosition, OrderPositionStatus } from '../../types'
import { cleanupDocument, clickElement, renderIntoDocument } from '../../testUtils/dom'
import { KitchenOrdersScreen, type KitchenBoardScreenStatus } from './KitchenOrdersScreen'

const mockKitchenBoard = vi.hoisted(() => ({
  actionError: null as string | null,
  advancePosition: vi.fn(),
  error: null as string | null,
  loading: false,
  pendingPositionIds: [] as string[],
  positionsByStatus: vi.fn(),
}))

vi.mock('../../kitchen/KitchenBoardContext', () => ({
  useKitchenBoard: () => mockKitchenBoard,
}))

const positions: OrderPosition[] = [
  {
    id: 'pos-new',
    orderNumber: '#101',
    title: 'Cortado',
    status: 'NEW',
    createdAt: '2026-07-19T08:00:00.000Z',
  },
  {
    id: 'pos-progress',
    orderNumber: '#102',
    title: 'Pour over',
    status: 'IN_PROGRESS',
    createdAt: '2026-07-19T08:01:00.000Z',
  },
  {
    id: 'pos-ready',
    orderNumber: '#103',
    title: 'Matcha',
    status: 'READY',
    createdAt: '2026-07-19T08:02:00.000Z',
  },
]

function positionsByStatus(status: OrderPositionStatus) {
  return positions.filter((position) => position.status === status)
}

describe('KitchenOrdersScreen', () => {
  beforeEach(() => {
    mockKitchenBoard.actionError = null
    mockKitchenBoard.advancePosition.mockReset()
    mockKitchenBoard.error = null
    mockKitchenBoard.loading = false
    mockKitchenBoard.pendingPositionIds = []
    mockKitchenBoard.positionsByStatus.mockReset()
    mockKitchenBoard.positionsByStatus.mockImplementation(positionsByStatus)
  })

  afterEach(async () => {
    await cleanupDocument()
  })

  const screenCases: Array<{
    status: KitchenBoardScreenStatus
    expectedTitle: string
  }> = [
    { status: 'NEW', expectedTitle: 'Cortado' },
    { status: 'IN_PROGRESS', expectedTitle: 'Pour over' },
    { status: 'READY', expectedTitle: 'Matcha' },
  ]

  for (const screenCase of screenCases) {
    it(`renders ${screenCase.status} positions from KitchenBoardContext`, () => {
      const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter={screenCase.status} />)

      expect(mockKitchenBoard.positionsByStatus).toHaveBeenCalledWith(screenCase.status)
      expect(markup).toContain(`data-status-filter="${screenCase.status}"`)
      expect(markup).toContain(screenCase.expectedTitle)
    })
  }

  it('renders loading state before the empty grid state', () => {
    mockKitchenBoard.loading = true
    mockKitchenBoard.positionsByStatus.mockReturnValue([])

    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="NEW" />)

    expect(markup).toContain('data-status-filter="NEW"')
    expect(markup).toContain('Loading orders')
    expect(markup).not.toContain('Очередь пуста')
    expect(mockKitchenBoard.positionsByStatus).not.toHaveBeenCalled()
  })

  it('renders error state before the empty grid state', () => {
    mockKitchenBoard.error = 'Load failed'
    mockKitchenBoard.positionsByStatus.mockReturnValue([])

    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="NEW" />)

    expect(markup).toContain('role="alert"')
    expect(markup).toContain('Unable to load orders')
    expect(markup).toContain('Load failed')
    expect(markup).not.toContain('Очередь пуста')
    expect(mockKitchenBoard.positionsByStatus).not.toHaveBeenCalled()
  })

  it('renders action errors without replacing loaded positions', () => {
    mockKitchenBoard.actionError = 'Advance failed'

    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="NEW" />)

    expect(markup).toContain('role="alert"')
    expect(markup).toContain('Unable to update order')
    expect(markup).toContain('Advance failed')
    expect(markup).toContain('Cortado')
    expect(mockKitchenBoard.positionsByStatus).toHaveBeenCalledWith('NEW')
  })

  it('disables cards with pending position updates', () => {
    mockKitchenBoard.pendingPositionIds = ['pos-new']

    const markup = renderToStaticMarkup(<KitchenOrdersScreen statusFilter="NEW" />)

    expect(markup).toContain('aria-busy="true"')
    expect(markup).toContain('disabled=""')
  })

  it('wires rendered card clicks to advancePosition', async () => {
    const { container } = await renderIntoDocument(<KitchenOrdersScreen statusFilter="NEW" />)
    const card = container.querySelector('[data-position-id="pos-new"]')

    expect(card).not.toBeNull()
    await clickElement(card!)

    expect(mockKitchenBoard.advancePosition).toHaveBeenCalledWith('pos-new', 'NEW')
  })
})
