import { describe, expect, it } from 'vitest'

import { pageForTick } from './useAutoRotatingPage'

const items = Array.from({ length: 20 }, (_, index) => index)

describe('pageForTick', () => {
  it('returns a single full page when everything fits', () => {
    const page = pageForTick([1, 2, 3], 8, 5)
    expect(page.pageCount).toBe(1)
    expect(page.pageIndex).toBe(0)
    expect(page.items).toEqual([1, 2, 3])
  })

  it('splits into pages of the given size', () => {
    expect(pageForTick(items, 8, 0).items).toEqual([0, 1, 2, 3, 4, 5, 6, 7])
    expect(pageForTick(items, 8, 1).items).toEqual([8, 9, 10, 11, 12, 13, 14, 15])
    expect(pageForTick(items, 8, 2).items).toEqual([16, 17, 18, 19])
  })

  it('wraps back to the first page after the last', () => {
    // 20 items / 8 per page = 3 pages; tick 3 wraps to page 0.
    expect(pageForTick(items, 8, 3).pageIndex).toBe(0)
    expect(pageForTick(items, 8, 4).pageIndex).toBe(1)
  })

  it('reports at least one page for an empty list', () => {
    const page = pageForTick([], 8, 0)
    expect(page.pageCount).toBe(1)
    expect(page.items).toEqual([])
  })
})
