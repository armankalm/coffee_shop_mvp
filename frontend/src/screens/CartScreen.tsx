import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ApiError, resolveAssetUrl } from '../api/client'
import { createOrder } from '../api/orders'
import { useCart } from '../cart/CartContext'
import { useShop } from '../shop/ShopContext'
import { Stepper } from '../components'
import heroFallback from '../assets/hero.png'
import styles from './Screens.module.css'

function formatMoney(amount: number) {
  return `${amount.toLocaleString('ru-RU')} ₸`
}

export function CartScreen() {
  const navigate = useNavigate()
  const { shop } = useShop()
  const { lines, updateQuantity, clearShop } = useCart()

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const shopLines = shop ? lines.filter((line) => line.shopId === shop.id) : []
  const total = shopLines.reduce((sum, line) => sum + (line.basePrice + line.toppingsPrice) * line.quantity, 0)

  async function handleCheckout() {
    if (!shop || shopLines.length === 0) return

    setIsSubmitting(true)
    setError(null)

    try {
      const order = await createOrder({
        shopId: shop.id,
        items: shopLines.map((line) => ({
          productId: line.productId,
          toppingIds: line.toppingIds,
          quantity: line.quantity,
        })),
      })
      clearShop(shop.id)
      navigate(`/order/${order.id}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Не удалось оформить заказ. Попробуйте ещё раз.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className={`${styles.screen} ${styles.cartScreen}`} aria-labelledby="cart-title">
      <header className={styles.cartHeader}>
        <Link className={styles.roundIconButton} to="/catalog" aria-label="Назад">
          ←
        </Link>
        <div className={styles.cartHeaderInfo}>
          <p className={styles.eyebrow}>📍 {shop?.name ?? 'Кофейня не выбрана'}</p>
        </div>
        <button
          className={styles.roundIconButton}
          type="button"
          onClick={() => {
            if (shop) clearShop(shop.id)
          }}
          disabled={shopLines.length === 0}
          aria-label="Очистить корзину"
        >
          🗑
        </button>
      </header>

      <h1 className={styles.visuallyHidden} id="cart-title">
        Корзина
      </h1>

      {shopLines.length === 0 ? (
        <p className={styles.muted}>Корзина пуста. Добавьте что-нибудь из каталога.</p>
      ) : (
        <div className={styles.list}>
          {shopLines.map((line) => (
            <article className={styles.cartCard} key={line.id}>
              <div className={styles.cartImageFrame}>
                <img
                  className={styles.cartImage}
                  src={resolveAssetUrl(line.imagePath) ?? heroFallback}
                  alt={line.productName}
                  draggable={false}
                />
              </div>
              <div className={styles.cartBody}>
                {line.toppingsLabel ? <p className={styles.muted}>{line.toppingsLabel}</p> : null}
                <p className={styles.cardTitle}>{line.productName}</p>
                <div className={styles.cartFooter}>
                  <span className={styles.price}>
                    {formatMoney((line.basePrice + line.toppingsPrice) * line.quantity)}
                  </span>
                  <Stepper
                    value={line.quantity}
                    min={0}
                    onDecrease={() => updateQuantity(line.id, line.quantity - 1)}
                    onIncrease={() => updateQuantity(line.id, line.quantity + 1)}
                  />
                </div>
              </div>
            </article>
          ))}
        </div>
      )}

      {error ? <p className={styles.muted}>{error}</p> : null}

      <div className={styles.cartBottomBar}>
        <div className={styles.cartTotalRow}>
          <span className={styles.muted}>Итого</span>
          <span className={styles.price}>{formatMoney(total)}</span>
        </div>
        <button
          className={styles.payButton}
          type="button"
          disabled={shopLines.length === 0 || !shop || isSubmitting}
          onClick={handleCheckout}
        >
          {isSubmitting ? 'Оформляем…' : 'Оформить заказ'}
        </button>
      </div>
    </section>
  )
}
