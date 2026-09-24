import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'

import type { OrderPosition } from '../../types'
import { OrderPositionsGrid } from './OrderPositionsGrid'

const positions: OrderPosition[] = [
  {
    id: 'older-ready',
    orderNumber: '#104',
    title: 'Flat white',
    status: 'READY',
    createdAt: '2026-07-19T08:00:00.000Z',
  },
  {
    id: 'newer-new',
    orderNumber: '#109',
    title: 'Iced americano',
    status: 'NEW',
    createdAt: '2026-07-19T08:05:00.000Z',
  },
]

describe('OrderPositionsGrid', () => {
  it('renders the received positions without filtering or reordering them', () => {
    const markup = renderToStaticMarkup(
      <OrderPositionsGrid
        statusFilter="READY"
        positions={positions}
        onPositionClick={() => undefined}
      />,
    )

    expect(markup).toContain('data-status-filter="READY"')
    expect(markup).toContain('Flat white')
    expect(markup).toContain('Iced americano')
    expect(markup.indexOf('Flat white')).toBeLessThan(markup.indexOf('Iced americano'))
  })

  it('renders each position as a clickable grid item with id and status metadata', () => {
    const markup = renderToStaticMarkup(
      <OrderPositionsGrid statusFilter="NEW" positions={positions} onPositionClick={() => undefined} />,
    )

    expect(markup).toContain('type="button"')
    expect(markup).toContain('data-position-layout="older-ready"')
    expect(markup).toContain('data-position-id="older-ready"')
    expect(markup).toContain('data-status="READY"')
    expect(markup).toContain('data-position-layout="newer-new"')
    expect(markup).toContain('data-position-id="newer-new"')
    expect(markup).toContain('data-status="NEW"')
  })

  it('disables cards with pending updates', () => {
    const markup = renderToStaticMarkup(
      <OrderPositionsGrid
        statusFilter="READY"
        positions={positions}
        pendingPositionIds={['older-ready']}
        onPositionClick={() => undefined}
      />,
    )

    expect(markup).toContain('aria-busy="true"')
    expect(markup).toContain('disabled=""')
  })

  it('renders an empty state when there are no positions', () => {
    const markup = renderToStaticMarkup(
      <OrderPositionsGrid statusFilter="NEW" positions={[]} onPositionClick={() => undefined} />,
    )

    expect(markup).toContain('role="status"')
    expect(markup).toContain('Очередь пуста')
    expect(markup).toContain('Новые позиции появятся здесь автоматически.')
    expect(markup).not.toContain('data-position-id=')
  })
})
