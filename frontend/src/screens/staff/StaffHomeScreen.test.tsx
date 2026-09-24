// @vitest-environment jsdom

import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'

import type { CoffeeShopDto } from '../../api/shops'
import { AuthProvider } from '../../auth/AuthContext'
import { StaffHomeScreen } from './StaffHomeScreen'

const mockState = vi.hoisted(() => ({
  shops: [] as CoffeeShopDto[],
  selectedShopId: null as number | null,
  selectShop: vi.fn(),
  loading: false,
  error: null as string | null,
}))

vi.mock('../../staff/useStaffShops', () => ({
  useStaffShops: () => mockState,
}))

const shop: CoffeeShopDto = {
  id: 5,
  name: 'Mega Park',
  address: 'ул. Розыбакиева, 247',
  status: 'OPEN',
  statusNameRu: 'Открыто',
  city: { id: 1, name: 'Алматы', region: 'Алматы' },
}

function render() {
  return renderToStaticMarkup(
    <MemoryRouter>
      <AuthProvider>
        <StaffHomeScreen />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('StaffHomeScreen', () => {
  it('links the board to the selected shop', () => {
    mockState.shops = [shop]
    mockState.selectedShopId = 5

    const markup = render()

    expect(markup).toContain('href="/board/5"')
    expect(markup).toContain('href="/orders"')
    expect(markup).toContain('href="/staff/pos"')
  })

  it('disables the board link when no shop is assigned', () => {
    mockState.shops = []
    mockState.selectedShopId = null

    const markup = render()

    expect(markup).not.toContain('href="/board/')
    expect(markup).toContain('Вам не назначена')
  })

  it('renders a logout control', () => {
    mockState.shops = [shop]
    mockState.selectedShopId = 5

    const markup = render()

    expect(markup).toContain('Выйти')
  })
})
