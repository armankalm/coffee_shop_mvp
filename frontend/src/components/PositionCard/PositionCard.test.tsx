// @vitest-environment jsdom

import { renderToStaticMarkup } from 'react-dom/server'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { OrderPosition } from '../../types'
import { cleanupDocument, clickElement, renderIntoDocument } from '../../testUtils/dom'
import { formatElapsedTime, PositionCard } from './index'

const position: OrderPosition = {
  id: 'position-104-1',
  orderNumber: '#104',
  title: 'Flat white 350 ml',
  status: 'NEW',
  createdAt: '2026-07-19T09:05:00.000Z',
  comment: 'Oat milk',
}

describe('PositionCard', () => {
  afterEach(async () => {
    await cleanupDocument()
    vi.useRealTimers()
  })

  it('renders title, order metadata, elapsed time, and highlighted comment', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-19T09:20:00.000Z'))

    const markup = renderToStaticMarkup(
      <PositionCard position={position} onPositionClick={() => undefined} />,
    )

    expect(markup).toContain('Flat white 350 ml')
    expect(markup).toContain('#104')
    expect(markup).toContain('15 мин')
    expect(markup).toContain('aria-label="Прошло 15 мин с создания"')
    expect(markup).toContain('Oat milk')
    expect(markup).toContain('data-position-id="position-104-1"')
    expect(markup).toContain('data-status="NEW"')
    expect(markup).toContain('type="button"')
  })

  it('formats elapsed time across minute, hour, and day ranges', () => {
    const nowMs = Date.parse('2026-07-19T09:20:00.000Z')

    expect(formatElapsedTime('2026-07-19T09:19:45.000Z', nowMs)).toBe('только что')
    expect(formatElapsedTime('2026-07-19T09:05:00.000Z', nowMs)).toBe('15 мин')
    expect(formatElapsedTime('2026-07-19T07:05:00.000Z', nowMs)).toBe('2 ч 15 мин')
    expect(formatElapsedTime('2026-07-17T08:20:00.000Z', nowMs)).toBe('2 д 1 ч')
  })

  it('calls onPositionClick with the card id', async () => {
    const onPositionClick = vi.fn()
    const { container } = await renderIntoDocument(
      <PositionCard position={position} onPositionClick={onPositionClick} />,
    )
    const card = container.querySelector('[data-position-id="position-104-1"]')

    expect(card).not.toBeNull()
    await clickElement(card!)

    expect(onPositionClick).toHaveBeenCalledWith('position-104-1')
  })
})
