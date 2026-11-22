import { useNavigate } from 'react-router-dom'

import type { ProductDto } from '../api/products'
import { useCart } from '../cart/CartContext'
import { useShop } from '../shop/ShopContext'
import { useToast } from '../toast/ToastContext'

/** Cart line id of a product without toppings (see CartContext.lineKey). */
export function plainLineId(shopId: number, productId: number) {
  return `${shopId}:${productId}:`
}

/**
 * «+» on a menu row (audit C3): products without options go straight to the cart with an undo toast,
 * products with toppings open their card so the guest can choose.
 */
export function useQuickAdd() {
  const navigate = useNavigate()
  const { shop } = useShop()
  const { lines, addItem, updateQuantity } = useCart()
  const { showToast } = useToast()

  function quantityOf(productId: number) {
    if (!shop) return 0
    return lines.find((line) => line.id === plainLineId(shop.id, productId))?.quantity ?? 0
  }

  function add(product: ProductDto) {
    if (!product.available) return
    if (!shop) {
      navigate('/locations')
      return
    }
    if (product.availableToppings.length > 0) {
      navigate(`/product/${product.id}`)
      return
    }
    const lineId = plainLineId(shop.id, product.id)
    const before = quantityOf(product.id)
    addItem(product, [], 1, shop.id)
    showToast({
      message: `${product.name} — в корзине`,
      action: { label: 'Отменить', onClick: () => updateQuantity(lineId, before) },
    })
  }

  function decrease(product: ProductDto) {
    if (!shop) return
    const lineId = plainLineId(shop.id, product.id)
    const current = quantityOf(product.id)
    updateQuantity(lineId, current - 1)
    if (current === 1) {
      showToast({
        message: `${product.name} убран из корзины`,
        action: { label: 'Вернуть', onClick: () => addItem(product, [], 1, shop.id) },
      })
    }
  }

  return { add, decrease, quantityOf }
}
