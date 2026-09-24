import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'

import App from './App'
import { AuthProvider } from './auth/AuthContext'
import { CartProvider } from './cart/CartContext'
import { FavoritesProvider } from './favorites/FavoritesContext'
import { ShopProvider } from './shop/ShopContext'

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

const testShop = {
  id: 1,
  name: 'ТРЦ Mega Park',
  city: { id: 1, name: 'Алматы', region: 'Алматы' },
  address: 'ул. Розыбакиева, 247А',
  status: 'ACTIVE',
  statusNameRu: 'Открыта',
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

describe('App', () => {
  beforeEach(() => {
    localStorage.setItem(
      'drinkit.auth',
      JSON.stringify({ accessToken: 'test-access', refreshToken: 'test-refresh', email: 'test@example.com', role: 'USER' }),
    )
    localStorage.removeItem('drinkit.shop')
    localStorage.removeItem('drinkit.cart')
  })

  it('renders the location route inside the safe-area layout', () => {
    const markup = renderRoute('/locations')

    expect(markup).toContain('safe-area')
    expect(markup).toContain('Выбор места заказа')
    expect(markup).toContain('placeholder="Поиск"')
    expect(markup).toContain('aria-label="Открыть карту"')
    expect(markup).toContain('aria-label="Закрыть выбор адреса"')
    expect(markup).toContain('aria-label="Найти ближайшую точку"')
  })

  it('does not render the catalog when no shop is selected yet', () => {
    expect(renderRoute('/catalog')).not.toContain('id="catalog-title"')
  })

  it('shows a loading state for the catalog once a shop is selected', () => {
    localStorage.setItem('drinkit.shop', JSON.stringify(testShop))

    const markup = renderRoute('/catalog')

    expect(markup).toContain('Каталог')
    expect(markup).toContain(testShop.name)
    expect(markup).toContain('Загружаем меню')
  })

  it('shows a loading state for the product detail screen', () => {
    const markup = renderRoute('/product/1')

    expect(markup).toContain('Загружаем товар')
  })

  it('shows a loading state for the profile screen', () => {
    const markup = renderRoute('/profile')

    expect(markup).toContain('aria-label="Назад"')
    expect(markup).toContain('aria-label="Открыть чат"')
    expect(markup).toContain('Загружаем профиль')
  })

  it('renders the completed cart screen layout', () => {
    const markup = renderRoute('/cart')

    expect(markup).toContain('Корзина пуста')
    expect(markup).toContain('aria-label="Очистить корзину"')
  })

  it('renders the kitchen orders route outside the customer bottom navigation', () => {
    localStorage.setItem(
      'drinkit.auth',
      JSON.stringify({
        accessToken: 'test-access',
        refreshToken: 'test-refresh',
        email: 'barista@example.com',
        role: 'BARISTA',
      }),
    )

    const markup = renderRoute('/orders/new')

    expect(markup).toContain('data-status-filter="NEW"')

    expect(markup).toContain('aria-label="Статусы заказов"')
    expect(markup).toContain('Новые · 0')
    expect(markup).not.toContain('aria-label="РћСЃРЅРѕРІРЅР°СЏ РЅР°РІРёРіР°С†РёСЏ"')
  })

  it('does not render the kitchen route for a user without the kitchen permission', () => {
    localStorage.setItem(
      'drinkit.auth',
      JSON.stringify({
        accessToken: 'test-access',
        refreshToken: 'test-refresh',
        email: 'customer@example.com',
        role: 'USER',
      }),
    )

    const markup = renderRoute('/orders/new')

    expect(markup).not.toContain('data-status-filter="NEW"')
  })

  it('does not render protected screen content for unauthenticated visitors', () => {
    localStorage.removeItem('drinkit.auth')

    const markup = renderRoute('/profile')

    expect(markup).not.toContain('История заказов')
  })
})
