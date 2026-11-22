// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act } from 'react'

import type { OrderPosition, OrderPositionStatus } from '../types'
import { cleanupDocument, clickElement, renderIntoDocument, waitFor } from '../testUtils/dom'

const mockPositionsApi = vi.hoisted(() => ({
  advancePositionStatus: vi.fn(),
  getPositions: vi.fn(),
  openKitchenBoardStream: vi.fn(() => ({ addEventListener: vi.fn(), close: vi.fn() })),
  toOrderPosition: vi.fn(),
  nextStatus: (status: OrderPositionStatus) => {
    const transitions: Record<OrderPositionStatus, OrderPositionStatus> = {
      NEW: 'IN_PROGRESS',
      IN_PROGRESS: 'READY',
      READY: 'COMPLETED',
      COMPLETED: 'COMPLETED',
    }
    return transitions[status]
  },
}))

vi.mock('../api/positions', () => mockPositionsApi)

// The kitchen board resolves a shop for its SSE stream via /me when none is
// stored; keep it offline in tests (no assigned shops → stream is skipped).
vi.mock('../api/user', () => ({
  getCurrentUser: vi.fn().mockResolvedValue({ assignedShops: [], coffeeShopId: null }),
}))

import { KitchenBoardProvider, useKitchenBoard } from './KitchenBoardContext'

const initialPositions: OrderPosition[] = [
  {
    id: 'pos-new',
    orderNumber: '#101',
    title: 'Cortado',
    status: 'NEW',
    createdAt: '2026-07-19T08:00:00.000Z',
  },
]

function KitchenBoardProbe() {
  const { actionError, advancePosition, counts, error, lastAction, loading, pendingPositionIds, positions, undoLastAction } =
    useKitchenBoard()
  const firstPosition = positions[0]
  const snapshot = [
    positions.length,
    String(loading),
    error ?? 'null',
    actionError ?? 'null',
    pendingPositionIds.join(',') || 'none',
    counts.NEW,
    counts.IN_PROGRESS,
    firstPosition?.status ?? 'none',
  ].join(':')

  return (
    <div>
      <output data-testid="snapshot">{snapshot}</output>
      <button
        type="button"
        onClick={() => {
          void advancePosition('pos-new', 'NEW')
        }}
      >
        advance
      </button>
      <button type="button" data-testid="undo" onClick={undoLastAction}>
        undo
      </button>
      <output data-testid="last-action">{lastAction?.message ?? 'no-action'}</output>
    </div>
  )
}

describe('KitchenBoardProvider async behavior', () => {
  beforeEach(() => {
    mockPositionsApi.advancePositionStatus.mockReset()
    mockPositionsApi.getPositions.mockReset()
  })

  afterEach(async () => {
    await cleanupDocument()
  })

  it('loads positions and exposes derived counts', async () => {
    mockPositionsApi.getPositions.mockResolvedValue(initialPositions)

    const { container } = await renderIntoDocument(
      <KitchenBoardProvider>
        <KitchenBoardProbe />
      </KitchenBoardProvider>,
    )

    await waitFor(() => {
      expect(container.textContent).toContain('1:false:null:null:none:1:0:NEW')
    })
  })

  it('stores load errors and stops loading', async () => {
    mockPositionsApi.getPositions.mockRejectedValue(new Error('load failed'))

    const { container } = await renderIntoDocument(
      <KitchenBoardProvider>
        <KitchenBoardProbe />
      </KitchenBoardProvider>,
    )

    await waitFor(() => {
      expect(container.textContent).toContain('0:false:load failed:null:none:0:0:none')
    })
  })

  it('rolls back an optimistic advance when the API rejects', async () => {
    mockPositionsApi.getPositions.mockResolvedValue(initialPositions)
    mockPositionsApi.advancePositionStatus.mockRejectedValue(new Error('advance failed'))

    const { container } = await renderIntoDocument(
      <KitchenBoardProvider undoDelayMs={0}>
        <KitchenBoardProbe />
      </KitchenBoardProvider>,
    )

    await waitFor(() => {
      expect(container.textContent).toContain('1:false:null:null:none:1:0:NEW')
    })

    const advanceButton = container.querySelector('button')
    expect(advanceButton).not.toBeNull()
    await clickElement(advanceButton!)

    await waitFor(() => {
      expect(container.textContent).toContain('1:false:null:advance failed:none:1:0:NEW')
    })
  })

  it('ignores duplicate and stale advances for the same position', async () => {
    const updatedPosition: OrderPosition = {
      id: 'pos-new',
      orderNumber: '#101',
      title: 'Cortado',
      status: 'IN_PROGRESS',
      createdAt: '2026-07-19T08:00:00.000Z',
    }
    let resolveAdvance: (position: OrderPosition) => void = () => undefined

    mockPositionsApi.getPositions.mockResolvedValue(initialPositions)
    mockPositionsApi.advancePositionStatus.mockReturnValue(
      new Promise<OrderPosition>((resolve) => {
        resolveAdvance = resolve
      }),
    )

    const { container } = await renderIntoDocument(
      <KitchenBoardProvider undoDelayMs={0}>
        <KitchenBoardProbe />
      </KitchenBoardProvider>,
    )

    await waitFor(() => {
      expect(container.textContent).toContain('1:false:null:null:none:1:0:NEW')
    })

    const advanceButton = container.querySelector('button')
    expect(advanceButton).not.toBeNull()
    await clickElement(advanceButton!)
    await clickElement(advanceButton!)

    expect(mockPositionsApi.advancePositionStatus).toHaveBeenCalledTimes(1)
    expect(mockPositionsApi.advancePositionStatus).toHaveBeenCalledWith('pos-new', 'NEW')

    await waitFor(() => {
      expect(container.textContent).toContain('1:false:null:null:pos-new:0:1:IN_PROGRESS')
    })

    await act(async () => {
      resolveAdvance(updatedPosition)
      await Promise.resolve()
    })

    await waitFor(() => {
      expect(container.textContent).toContain('1:false:null:null:none:0:1:IN_PROGRESS')
    })

    await clickElement(advanceButton!)

    expect(mockPositionsApi.advancePositionStatus).toHaveBeenCalledTimes(1)
  })

  it('moves the card at once but sends the change only after the undo window', async () => {
    mockPositionsApi.getPositions.mockResolvedValue(initialPositions)
    mockPositionsApi.advancePositionStatus.mockResolvedValue({ ...initialPositions[0]!, status: 'IN_PROGRESS' })

    const { container } = await renderIntoDocument(
      <KitchenBoardProvider undoDelayMs={60}>
        <KitchenBoardProbe />
      </KitchenBoardProvider>,
    )
    await waitFor(() => expect(container.textContent).toContain('1:false:null:null:none:1:0:NEW'))

    await clickElement(container.querySelector('button')!)

    expect(container.textContent).toContain('1:false:null:null:pos-new:0:1:IN_PROGRESS')
    expect(container.textContent).toContain('#101 Cortado → В работе')
    expect(mockPositionsApi.advancePositionStatus).not.toHaveBeenCalled()

    await waitFor(() => expect(mockPositionsApi.advancePositionStatus).toHaveBeenCalledWith('pos-new', 'NEW'))
    await waitFor(() => expect(container.textContent).toContain('1:false:null:null:none:0:1:IN_PROGRESS'))
    expect(container.textContent).toContain('no-action')
  })

  it('undo restores the card and never calls the API', async () => {
    mockPositionsApi.getPositions.mockResolvedValue(initialPositions)

    const { container } = await renderIntoDocument(
      <KitchenBoardProvider undoDelayMs={60}>
        <KitchenBoardProbe />
      </KitchenBoardProvider>,
    )
    await waitFor(() => expect(container.textContent).toContain('1:false:null:null:none:1:0:NEW'))

    await clickElement(container.querySelector('button')!)
    await clickElement(container.querySelector('[data-testid="undo"]')!)

    expect(container.textContent).toContain('1:false:null:null:none:1:0:NEW')
    expect(container.textContent).toContain('no-action')
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 120))
    })
    expect(mockPositionsApi.advancePositionStatus).not.toHaveBeenCalled()
  })
})
