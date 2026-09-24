import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiGet, apiPatch } from './client'
import { advancePositionStatus, getPositions, nextStatus, type OrderItemBoardDto } from './positions'

vi.mock('./client', () => ({
  apiGet: vi.fn(),
  apiPatch: vi.fn(),
}))

const mockedApiGet = vi.mocked(apiGet)
const mockedApiPatch = vi.mocked(apiPatch)

function boardDto(overrides: Partial<OrderItemBoardDto> = {}): OrderItemBoardDto {
  return {
    id: 42,
    orderId: 104,
    shopId: 1,
    orderNumber: '№104',
    title: 'Латте',
    quantity: 1,
    status: 'NEW',
    statusNameRu: 'Новый',
    createdAt: '2026-07-19T08:00:00Z',
    comment: null,
    ...overrides,
  }
}

describe('positions api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('maps position statuses to the next kitchen-board step', () => {
    expect(nextStatus('NEW')).toBe('IN_PROGRESS')
    expect(nextStatus('IN_PROGRESS')).toBe('READY')
    expect(nextStatus('READY')).toBe('COMPLETED')
    expect(nextStatus('COMPLETED')).toBe('COMPLETED')
  })

  it('loads positions from the admin order-items endpoint and maps them', async () => {
    mockedApiGet.mockResolvedValue([
      boardDto({ id: 7, quantity: 2, comment: 'Корица' }),
    ])

    const positions = await getPositions()

    expect(mockedApiGet).toHaveBeenCalledWith('/admin/order-items')
    expect(positions).toEqual([
      {
        id: '7',
        orderNumber: '№104',
        title: 'Латте ×2',
        status: 'NEW',
        createdAt: '2026-07-19T08:00:00Z',
        comment: 'Корица',
      },
    ])
  })

  it('omits comment when the backend returns null', async () => {
    mockedApiGet.mockResolvedValue([boardDto({ comment: null })])

    const [position] = await getPositions()

    expect(position).not.toHaveProperty('comment')
  })

  it('advances a position by PATCHing the target status', async () => {
    mockedApiPatch.mockResolvedValue(boardDto({ id: 42, status: 'IN_PROGRESS' }))

    const updated = await advancePositionStatus('42', 'NEW')

    expect(mockedApiPatch).toHaveBeenCalledWith('/admin/order-items/42/status', {
      statusCode: 'IN_PROGRESS',
    })
    expect(updated.status).toBe('IN_PROGRESS')
    expect(updated.id).toBe('42')
  })

  it('refuses to advance a position that is already completed', async () => {
    await expect(advancePositionStatus('42', 'COMPLETED')).rejects.toThrow('already COMPLETED')
    expect(mockedApiPatch).not.toHaveBeenCalled()
  })

  it('propagates backend errors (e.g. unknown id)', async () => {
    mockedApiPatch.mockRejectedValue(new Error('Order item not found: 999'))

    await expect(advancePositionStatus('999', 'NEW')).rejects.toThrow('not found')
  })
})
