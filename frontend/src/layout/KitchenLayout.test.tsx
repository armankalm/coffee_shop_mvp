import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import type { PositionCounts } from '../kitchen/KitchenBoardContext'
import { KitchenLayoutShell } from './KitchenLayout'

const counts: PositionCounts = {
  NEW: 3,
  IN_PROGRESS: 2,
  READY: 1,
  COMPLETED: 4,
}

function renderKitchenLayout(path: string) {
  return renderToStaticMarkup(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/orders" element={<KitchenLayoutShell counts={counts} />}>
          <Route path="new" element={<span>New orders outlet</span>} />
          <Route path="in-progress" element={<span>In-progress orders outlet</span>} />
          <Route path="ready" element={<span>Ready orders outlet</span>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('KitchenLayout', () => {
  it('renders tab counts and nested route content', () => {
    const markup = renderKitchenLayout('/orders/in-progress')

    expect(markup).toContain('Новые · 3')
    expect(markup).toContain('В работе · 2')
    expect(markup).toContain('Готовы · 1')
    expect(markup).toContain('In-progress orders outlet')
  })

  it('marks the active kitchen tab', () => {
    const markup = renderKitchenLayout('/orders/ready')

    expect(markup).toContain('aria-current="page"')
    expect(markup).toContain('href="/orders/ready"')
  })
})
