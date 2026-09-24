import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { ApiError, resolveAssetUrl } from '../api/client'
import type { ProductDto } from '../api/products'
import { getProductById } from '../api/products'
import { useCart } from '../cart/CartContext'
import { HScroll } from '../components'
import { useFavorites } from '../favorites/FavoritesContext'
import { useShop } from '../shop/ShopContext'
import heroFallback from '../assets/hero.png'
import styles from './Screens.module.css'

type LoadState =
  | { status: 'loading'; productId: string | undefined }
  | { status: 'error'; productId: string | undefined; message: string }
  | { status: 'ready'; productId: string; product: ProductDto }

function formatMoney(amount: number) {
  return `${amount.toLocaleString('ru-RU')} ₸`
}

export function ProductScreen() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const { addItem } = useCart()
  const { isFavorite, toggleFavorite } = useFavorites()
  const { shop } = useShop()

  const [state, setState] = useState<LoadState>(() => ({ status: 'loading', productId }))
  const [selectedToppings, setSelectedToppings] = useState<{ productId: string | undefined; ids: number[] }>(() => ({
    productId,
    ids: [],
  }))
  const selectedToppingIds = useMemo(
    () => (selectedToppings.productId === productId ? selectedToppings.ids : []),
    [productId, selectedToppings.ids, selectedToppings.productId],
  )

  useEffect(() => {
    if (!productId) return

    let cancelled = false
    const requestedProductId = productId

    getProductById(Number(requestedProductId))
      .then((data) => {
        if (!cancelled) setState({ status: 'ready', productId: requestedProductId, product: data })
      })
      .catch((err) => {
        if (!cancelled) {
          setState({
            status: 'error',
            productId: requestedProductId,
            message: err instanceof ApiError ? err.message : 'Не удалось загрузить товар.',
          })
        }
      })

    return () => {
      cancelled = true
    }
  }, [productId])

  const product = state.status === 'ready' && state.productId === productId ? state.product : null

  const validSelectedToppingIds = useMemo(() => {
    if (!product) return []

    const availableToppingIds = new Set(product.availableToppings.map((topping) => topping.id))
    return selectedToppingIds.filter((toppingId) => availableToppingIds.has(toppingId))
  }, [product, selectedToppingIds])

  const toppingsPrice = useMemo(() => {
    if (!product) return 0
    const selectedIds = new Set(validSelectedToppingIds)
    return product.availableToppings
      .filter((topping) => selectedIds.has(topping.id))
      .reduce((sum, topping) => sum + topping.price, 0)
  }, [product, validSelectedToppingIds])

  if (state.status === 'loading' || state.productId !== productId) {
    return (
      <section className={`${styles.screen} ${styles.productScreen}`}>
        <p className={styles.productDescription}>Загружаем товар…</p>
      </section>
    )
  }

  if (state.status === 'error' || !product) {
    return (
      <section className={`${styles.screen} ${styles.productScreen}`}>
        <p className={styles.productDescription}>{state.status === 'error' ? state.message : 'Товар не найден.'}</p>
        <Link to="/catalog">Вернуться в каталог</Link>
      </section>
    )
  }

  const totalPrice = product.basePrice + toppingsPrice

  function toggleTopping(toppingId: number) {
    if (!product) return
    const topping = product.availableToppings.find((entry) => entry.id === toppingId)
    if (!topping) return

    setSelectedToppings((current) => {
      const currentIds = current.productId === productId ? current.ids : []

      if (currentIds.includes(toppingId)) {
        return { productId, ids: currentIds.filter((id) => id !== toppingId) }
      }
      const withoutIncompatible = currentIds.filter((id) => !topping.incompatibleWithIds.includes(id))
      return { productId, ids: [...withoutIncompatible, toppingId] }
    })
  }

  function handleAddToCart() {
    if (!product || !product.available) return
    if (!shop) {
      navigate('/locations')
      return
    }
    addItem(product, validSelectedToppingIds, 1, shop.id)
    navigate('/cart')
  }

  const toppingsByType = new Map<string, typeof product.availableToppings>()
  for (const topping of product.availableToppings) {
    const group = toppingsByType.get(topping.typeNameRu) ?? []
    group.push(topping)
    toppingsByType.set(topping.typeNameRu, group)
  }

  return (
    <section className={`${styles.screen} ${styles.productScreen}`} aria-labelledby="product-title">
      <div className={styles.productHero}>
        <img
          className={styles.productHeroImage}
          src={resolveAssetUrl(product.imagePath) ?? heroFallback}
          alt={product.name}
          draggable={false}
        />
        <div className={styles.productHeroShade} aria-hidden="true" />

        <div className={styles.productTopControls}>
          <button
            aria-label={isFavorite(product.id) ? 'Убрать из избранного' : 'Добавить в избранное'}
            aria-pressed={isFavorite(product.id)}
            className={styles.productIconButton}
            onClick={() => toggleFavorite(product)}
            type="button"
          >
            {isFavorite(product.id) ? '♥' : '♡'}
          </button>
          <h1 className={styles.productTopTitle} id="product-title">
            {product.name}
          </h1>
          <Link className={styles.productIconButton} to="/catalog" aria-label="Закрыть карточку товара">
            ×
          </Link>
        </div>
      </div>

      <div className={styles.productContent}>
        {toppingsByType.size > 0 ? (
          <HScroll className={styles.productModifierScroll} aria-label="Топинги">
            <div className={styles.productModifierRail}>
              {Array.from(toppingsByType.entries()).map(([typeTitle, toppings]) =>
                toppings.map((topping) => {
                  const isSelected = selectedToppingIds.includes(topping.id)

                  return (
                    <button
                      aria-pressed={isSelected}
                      className={styles.productModifierCard}
                      key={topping.id}
                      onClick={() => toggleTopping(topping.id)}
                      type="button"
                    >
                      <span className={styles.productModifierTitle}>{topping.name}</span>
                      <span className={styles.productModifierFooter}>
                        <span className={styles.productModifierHint}>{typeTitle}</span>
                        <span className={styles.productModifierPrice}>
                          {isSelected ? '✓ ' : '+ '}
                          {formatMoney(topping.price)}
                        </span>
                      </span>
                    </button>
                  )
                }),
              )}
            </div>
          </HScroll>
        ) : null}
      </div>

      <div className={styles.productBottomBar}>
        <button
          className={styles.productAddButton}
          onClick={handleAddToCart}
          type="button"
          disabled={!product.available}
          aria-label={`Добавить ${product.name} в корзину`}
        >
          {product.available ? `+ ${formatMoney(totalPrice)}` : 'Unavailable'}
        </button>
      </div>
    </section>
  )
}
