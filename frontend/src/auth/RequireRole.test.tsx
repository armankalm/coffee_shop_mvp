import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'

import { AuthProvider, type AuthSession } from './AuthContext'
import { RequireAuth } from './RequireAuth'
import { RequireRole } from './RequireRole'
import { KITCHEN_BOARD_PERMISSION } from './permissions'

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

function storeSession(role: string, permissions?: string[]) {
  const session: AuthSession = {
    accessToken: 'test-access',
    refreshToken: 'test-refresh',
    email: 'test@example.com',
    role,
  }

  if (permissions) {
    session.permissions = permissions
  }

  localStorage.setItem('drinkit.auth', JSON.stringify(session))
}

function renderProtectedRoute() {
  return renderToStaticMarkup(
    <MemoryRouter initialEntries={['/orders/new']}>
      <AuthProvider>
        <Routes>
          <Route element={<RequireAuth />}>
            <Route element={<RequireRole permission={KITCHEN_BOARD_PERMISSION} />}>
              <Route path="orders/new" element={<span>Kitchen board</span>} />
            </Route>
            <Route index element={<span>Home page</span>} />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('RequireRole', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('renders protected content when the role has the permission', () => {
    storeSession('BARISTA')

    expect(renderProtectedRoute()).toContain('Kitchen board')
  })

  it('does not render protected content when the role lacks the permission', () => {
    storeSession('USER')

    expect(renderProtectedRoute()).not.toContain('Kitchen board')
  })

  it('ignores malformed stored sessions', () => {
    localStorage.setItem(
      'drinkit.auth',
      JSON.stringify({
        role: 'BARISTA',
      }),
    )

    expect(renderProtectedRoute()).not.toContain('Kitchen board')
  })

  it('allows backend-provided permissions to grant access without changing the UI guard', () => {
    storeSession('USER', [KITCHEN_BOARD_PERMISSION])

    expect(renderProtectedRoute()).toContain('Kitchen board')
  })
})
