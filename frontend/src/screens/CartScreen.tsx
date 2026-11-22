import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ApiError } from '../api/client'
import { createOrder } from '../api/orders'
import { useCart, type CartLine } from '../cart/CartContext'
import { Icon } from '../components'
import { ProductPhoto } from '../components/ProductPhoto'
import { formatMoney, pluralPositions } from '../format'
import { useShop } from '../shop/ShopContext'
import { useToast } from '../toast/ToastContext'
import { isShopOpen } from '../shop/shopStatus'
import { ShopLine } from './ShopLine'
import styles from './Cream.module.css'

function lineTotal(line: CartLine) {
  return (line.basePrice + line.toppingsPrice) * line.quantity
}

export function CartScreen() {
  const navigate = useNavigate()
  const { shop } = useShop()
  const { lines, updateQuantity, clearShop, removeItem, restoreLines } = useCart()
  const { showToast } = useToast()

  const [sheetOpen, setSheetOpen] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const shopLines = shop ? lines.filter((line) => line.shopId === shop.id) : []
  const itemCount = shopLines.reduce((sum, line) => sum + line.quantity, 0)
  const total = shopLines.reduce((sum, line) => sum + lineTotal(line), 0)

  // Destructive actions are undoable instead of silent (audit K1/K2).
  function handleClear() {
    if (!shop || shopLines.length === 0) return
    const snapshot = shopLines
    clearShop(shop.id)
    showToast({ message: 'Корзина очищена', action: { label: 'Вернуть', onClick: () => restoreLines(snapshot) } })
  }

  function handleDecrease(line: CartLine) {
    if (line.quantity > 1) {
      updateQuantity(line.id, line.quantity - 1)
      return
    }
    removeItem(line.id)
    showToast({
      message: `${line.productName} убран из корзины`,
      action: { label: 'Вернуть', onClick: () => restoreLines([line]) },
    })
  }

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
        <h1 className={styles.pageTitle} id="cart-title">
          Корзина
        </h1>
        <button
          className={styles.iconButton}
          type="button"
          onClick={handleClear}
          disabled={shopLines.length === 0}
          aria-label="Очистить корзину"
        >
          <Icon name="trash" />
        </button>
      </header>

      {shop ? <ShopLine shop={shop} /> : null}

      {shopLines.length === 0 ? (
        <div className={styles.empty}>
          <span className={styles.emptyIcon}>
            <Icon name="bag" size={30} />
          </span>
          <p className={styles.emptyTitle}>Корзина пуста</p>
          <p className={styles.muted}>Загляните в меню — там тепло и пахнет кофе.</p>
          <Link className={`${styles.btn} ${styles.btnPrimary}`} to={shop ? '/catalog' : '/locations'}>
            В меню
          </Link>
        </div>
      ) : (
        <div>
          {shopLines.map((line) => {
            const editHref = `/product/${line.productId}?line=${encodeURIComponent(line.id)}`
            return (
              <article className={styles.cartLine} key={line.id}>
                <Link to={editHref} aria-label={`Изменить ${line.productName}`} tabIndex={-1}>
                  <ProductPhoto className={styles.photo} imagePath={line.imagePath} alt={line.productName} width={72} />
                </Link>
                <div className={styles.cartLineBody}>
                  <Link className={styles.cartLineTitle} to={editHref}>
                    {line.productName}
                  </Link>
                  <p className={styles.cartLineToppings}>{line.toppingsLabel || 'Без добавок'}</p>
                  <div className={styles.cartLineFooter}>
                    <b>{formatMoney(lineTotal(line))}</b>
                    <div className={`${styles.qty} ${styles.qtySoft}`} role="group" aria-label={`Количество: ${line.productName}`}>
                      <button
                        type="button"
                        aria-label={line.quantity === 1 ? `Удалить ${line.productName}` : 'Уменьшить количество'}
                        onClick={() => handleDecrease(line)}
                      >
                        <Icon name={line.quantity === 1 ? 'trash' : 'minus'} size={16} />
                      </button>
                      <span>{line.quantity}</span>
                      <button
                        type="button"
                        aria-label="Увеличить количество"
                        onClick={() => updateQuantity(line.id, line.quantity + 1)}
                      >
                        <Icon name="plus" size={16} />
                      </button>
                    </div>
                  </div>
                </div>
              </article>
            )
          })}
        </div>
      )}

      {shopLines.length > 0 ? (
        <div className={styles.checkoutDock} data-toast-lift>
          <div className={styles.summary}>
            <span className={styles.muted}>{pluralPositions(itemCount)}</span>
            <b>{formatMoney(total)}</b>
          </div>
          <button
            className={`${styles.btn} ${styles.btnPrimary} ${styles.btnBlock}`}
            type="button"
            disabled={!shop}
            onClick={() => {
              setError(null)
              setSheetOpen(true)
            }}
          >
            Оформить заказ
          </button>
        </div>
      ) : null}

      {sheetOpen && shop ? (
        <CheckoutSheet
          onClose={() => setSheetOpen(false)}
          onConfirm={handleCheckout}
          shopName={shop.name}
          shopAddress={shop.address}
          shopOpen={isShopOpen(shop)}
          shopStatus={shop.statusNameRu}
          itemCount={itemCount}
          total={total}
          error={error}
          isSubmitting={isSubmitting}
        />
      ) : null}
    </section>
  )
}

type CheckoutSheetProps = {
  onClose: () => void
  onConfirm: () => void
  shopName: string
  shopAddress: string
  shopOpen: boolean
  shopStatus: string
  itemCount: number
  total: number
  error: string | null
  isSubmitting: boolean
}

function CheckoutSheet({
  onClose,
  onConfirm,
  shopName,
  shopAddress,
  shopOpen,
  shopStatus,
  itemCount,
  total,
  error,
  isSubmitting,
}: CheckoutSheetProps) {
  const sheetRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    sheetRef.current?.focus()
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <>
      <div className={styles.dim} onClick={onClose} aria-hidden="true" />
      <div
        className={styles.sheet}
        role="dialog"
        aria-modal="true"
        aria-labelledby="checkout-title"
        tabIndex={-1}
        ref={sheetRef}
      >
        <div className={styles.grab} aria-hidden="true" />
        <h2 className={styles.sheetTitle} id="checkout-title">
          Оформление
        </h2>

        <Link className={styles.optRow} to="/locations" state={{ from: '/cart' }}>
          <span className={styles.optIcon}>
            <Icon name="pin" />
          </span>
          <span>
            {shopName}
            <small>
              Самовывоз · {shopAddress} · {shopStatus}
            </small>
          </span>
          <Icon name="right" size={16} className={styles.optChevron} />
        </Link>

        <div className={styles.optRow}>
          <span className={styles.optIcon}>
            <Icon name="clock" />
          </span>
          <span>
            Как можно скорее
            <small>Начнём готовить сразу после оформления</small>
          </span>
        </div>

        {!shopOpen ? (
          <p className={`${styles.alert} ${styles.sheetAlert}`} role="alert">
            <Icon name="alert" size={16} />
            Кофейня сейчас закрыта — заказ может не принять бариста.
          </p>
        ) : null}

        {error ? (
          <p className={`${styles.alert} ${styles.sheetAlert}`} role="alert">
            <Icon name="alert" size={16} />
            {error}
          </p>
        ) : null}

        <div className={styles.sheetActions}>
          <div className={styles.summary}>
            <span className={styles.muted}>{pluralPositions(itemCount)}</span>
            <b>{formatMoney(total)}</b>
          </div>
          <button className={styles.cta} type="button" onClick={onConfirm} disabled={isSubmitting}>
            <span>{isSubmitting ? 'Оформляем…' : 'Подтвердить заказ'}</span>
            <span>{formatMoney(total)}</span>
          </button>
          <button className={`${styles.btn} ${styles.btnGhost}`} type="button" onClick={onClose}>
            Вернуться к корзине
          </button>
        </div>
      </div>
    </>
  )
}
