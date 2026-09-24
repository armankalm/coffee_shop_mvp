import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import App from './App'
import { AuthProvider } from './auth/AuthContext'
import { CartProvider } from './cart/CartContext'
import { FavoritesProvider } from './favorites/FavoritesContext'
import { ShopProvider } from './shop/ShopContext'

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  const React = await import('react')

  return {
    ...actual,
    Navigate: ({ replace, to }: { replace?: boolean; to: string }) =>
      React.createElement('span', {
        'data-replace': replace ? 'true' : 'false',
        'data-route-redirect': to,
      }),
  }
})

class MemoryStorage implements Storage {
  private store = new Map<string, string>()

  get length() {
    return this.store.size
  }

  clear = () => this.store.clear()
  getItem = (key: string) => this.store.get(key) ?? null
  key = (index: number) => Array.from(this.store.keys())[index] ?? null
  removeItem = (key: string) => void this.store.delete(key)
  setItem = (key: string, value: string) => void this.store.set(key, value)
}

globalThis.localStorage ??= new MemoryStorage()

function storeSession(role: string) {
  localStorage.setItem(
    'drinkit.auth',
    JSON.stringify({
      accessToken: 'test-access',
      refreshToken: 'test-refresh',
      email: `${role.toLowerCase()}@example.com`,
      role,
    }),
  )
}

function renderRoute(route: string) {
  return renderToStaticMarkup(
    <MemoryRouter initialEntries={[route]}>
      <AuthProvider>
        <ShopProvider>
          <FavoritesProvider>
            <CartProvider>
              <App />
            </CartProvider>
          </FavoritesProvider>
        </ShopProvider>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('App kitchen routes', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('redirects /orders to the new orders tab', () => {
    storeSession('BARISTA')

    const markup = renderRoute('/orders')

    expect(markup).toContain('data-route-redirect="new"')
    expect(markup).toContain('data-replace="true"')
  })

  it('mounts each kitchen status screen under the kitchen layout', () => {
    storeSession('BARISTA')

    expect(renderRoute('/orders/new')).toContain('data-status-filter="NEW"')
    expect(renderRoute('/orders/in-progress')).toContain('data-status-filter="IN_PROGRESS"')
    expect(renderRoute('/orders/ready')).toContain('data-status-filter="READY"')
  })

  it('redirects users without the kitchen permission away from orders', () => {
    storeSession('USER')

    const markup = renderRoute('/orders/new')

    expect(markup).toContain('data-route-redirect="/"')
    expect(markup).not.toContain('data-status-filter="NEW"')
  })
})
