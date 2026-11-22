import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import type { OrderDto } from '../api/orders'
import { getProductById } from '../api/products'
import { useCart } from '../cart/CartContext'
import { useShop } from '../shop/ShopContext'
import { useToast } from '../toast/ToastContext'

/** «Повторить» a past order in one tap: re-adds what is still available and opens the cart. */
export function useRepeatOrder() {
  const navigate = useNavigate()
  const { addItem } = useCart()
  const { shop } = useShop()
  const { showToast } = useToast()
  const [repeatingOrderId, setRepeatingOrderId] = useState<number | null>(null)
  const [repeatError, setRepeatError] = useState<string | null>(null)

  async function repeat(order: OrderDto) {
    setRepeatError(null)

    if (!shop || shop.id !== order.shopId) {
      setRepeatError(`Этот заказ был в «${order.shopName}». Выберите эту кофейню, чтобы повторить его.`)
      return
    }

    setRepeatingOrderId(order.id)
    try {
      const results = await Promise.all(
        order.items.map(async (item) => {
          try {
            return { item, product: await getProductById(item.productId) }
          } catch {
            return { item, product: null }
          }
        }),
      )

      let added = 0
      for (const { item, product } of results) {
        if (!product || !product.available) continue
        addItem(
          product,
          item.toppings.map((topping) => topping.id),
          item.quantity,
          shop.id,
        )
        added += 1
      }

      if (added === 0) {
        setRepeatError('Все товары из этого заказа сейчас недоступны.')
        return
      }
      if (added < order.items.length) {
        showToast({ message: 'Часть товаров сейчас недоступна — добавили остальное' })
      }
      navigate('/cart')
    } finally {
      setRepeatingOrderId(null)
    }
  }

  return { repeat, repeatingOrderId, repeatError }
}
