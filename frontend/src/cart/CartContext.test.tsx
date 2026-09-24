// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it } from 'vitest'

import type { ProductDto } from '../api/products'
import { cleanupDocument, clickElement, renderIntoDocument, waitFor } from '../testUtils/dom'
import { CartProvider, useCart } from './CartContext'

const baseProduct: ProductDto = {
  id: 10,
  name: 'Iced latte',
  category: 'coffee',
  categoryNameRu: 'Coffee',
  basePrice: 2600,
  available: true,
  imagePath: null,
  description: null,
  availableToppings: [],
}

const secondProduct: ProductDto = {
  ...baseProduct,
  id: 11,
  name: 'Flat white',
  basePrice: 1800,
}

const toppingProduct: ProductDto = {
  ...baseProduct,
  id: 12,
  name: 'Mocha',
  availableToppings: [
    {
      id: 1,
      name: 'Vanilla',
      type: 'SYRUP',
      typeNameRu: 'Syrup',
      price: 200,
      incompatibleWithIds: [],
    },
  ],
}

function CartProbe() {
  const { addItem, clearShop, lines } = useCart()

  return (
    <div>
      <output>
        {lines.map((line) => `${line.shopId}:${line.productId}:${line.quantity}:${line.toppingIds.join(',') || 'none'}`).join('|') ||
          'empty'}
      </output>
      <button
        type="button"
        onClick={() => {
          addItem(baseProduct, [], 1, 1)
          addItem(secondProduct, [], 2, 1)
        }}
      >
        repeat
      </button>
      <button
        type="button"
        onClick={() => {
          addItem(baseProduct, [], 1, 2)
        }}
      >
        other shop
      </button>
      <button
        type="button"
        onClick={() => {
          addItem(toppingProduct, [1, 999], 1, 1)
        }}
      >
        invalid topping
      </button>
      <button type="button" onClick={() => clearShop(1)}>
        clear shop
      </button>
    </div>
  )
}

describe('CartProvider', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(async () => {
    await cleanupDocument()
  })

  it('applies multiple addItem calls from one event without losing earlier lines', async () => {
    const { container } = await renderIntoDocument(
      <CartProvider>
        <CartProbe />
      </CartProvider>,
    )

    await clickElement(container.querySelector('button')!)

    await waitFor(() => {
      expect(container.textContent).toContain('1:10:1:none|1:11:2:none')
    })
  })

  it('stores shop ids in cart line keys', async () => {
    const { container } = await renderIntoDocument(
      <CartProvider>
        <CartProbe />
      </CartProvider>,
    )
    const buttons = container.querySelectorAll('button')

    await clickElement(buttons[0]!)
    await clickElement(buttons[1]!)

    await waitFor(() => {
      expect(container.textContent).toContain('1:10:1:none|1:11:2:none|2:10:1:none')
    })
  })

  it('stores only toppings available for the selected product', async () => {
    const { container } = await renderIntoDocument(
      <CartProvider>
        <CartProbe />
      </CartProvider>,
    )
    const buttons = container.querySelectorAll('button')

    await clickElement(buttons[2]!)

    await waitFor(() => {
      expect(container.textContent).toContain('1:12:1:1')
      expect(container.textContent).not.toContain('999')
    })
  })

  it('clears only lines for the requested shop', async () => {
    const { container } = await renderIntoDocument(
      <CartProvider>
        <CartProbe />
      </CartProvider>,
    )
    const buttons = container.querySelectorAll('button')

    await clickElement(buttons[0]!)
    await clickElement(buttons[1]!)
    await clickElement(buttons[3]!)

    await waitFor(() => {
      expect(container.textContent).toContain('2:10:1:none')
      expect(container.textContent).not.toContain('1:10')
      expect(container.textContent).not.toContain('1:11')
    })
  })

  it('drops malformed persisted cart lines', async () => {
    localStorage.setItem(
      'drinkit.cart',
      JSON.stringify([
        { id: 'bad', shopId: 1 },
        {
          id: '1:10:',
          shopId: 1,
          productId: 10,
          productName: 'Iced latte',
          imagePath: null,
          basePrice: 2600,
          toppingIds: [],
          toppingsLabel: '',
          toppingsPrice: 0,
          quantity: 1,
        },
      ]),
    )

    const { container } = await renderIntoDocument(
      <CartProvider>
        <CartProbe />
      </CartProvider>,
    )

    expect(container.textContent).toContain('1:10:1:none')
    expect(container.textContent).not.toContain('bad')
  })
})
