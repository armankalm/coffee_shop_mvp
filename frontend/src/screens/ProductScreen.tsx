import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'

import { ApiError } from '../api/client'
import type { ProductDto } from '../api/products'
import { getProductById } from '../api/products'
import { useCart } from '../cart/CartContext'
import { Icon, Skeleton, SkeletonStatus } from '../components'
import { ProductPhoto } from '../components/ProductPhoto'
import { useFavorites } from '../favorites/FavoritesContext'
import { formatMoney } from '../format'
import { groupToppings, toggleTopping } from '../menu/toppingGroups'
import { useShop } from '../shop/ShopContext'
import { useToast } from '../toast/ToastContext'
import styles from './Cream.module.css'

type LoadState =
  | { status: 'loading'; productId: string | undefined }
  | { status: 'error'; productId: string | undefined; message: string }
  | { status: 'ready'; productId: string; product: ProductDto }

type Selection = { productId: string | undefined; ids: number[]; hint: string | null }

export function ProductScreen() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { addItem, replaceItem, updateQuantity, lines } = useCart()
  const [searchParams] = useSearchParams()
  // Opened from the cart (?line=<cart line id>): edit that line instead of adding a new one.
  const editingLine = lines.find((line) => line.id === searchParams.get('line') && String(line.productId) === productId)
  const { isFavorite, toggleFavorite } = useFavorites()
  const { shop } = useShop()
  const { showToast } = useToast()

  const [state, setState] = useState<LoadState>(() => ({ status: 'loading', productId }))
  const [selection, setSelection] = useState<Selection>(() => ({
    productId,
    ids: editingLine?.toppingIds ?? [],
    hint: null,
  }))
  const [quantity, setQuantity] = useState({ productId, value: 1 })

  const selectedIds = useMemo(
    () => (selection.productId === productId ? selection.ids : []),
    [productId, selection.ids, selection.productId],
  )
  const hint = selection.productId === productId ? selection.hint : null
  const count = quantity.productId === productId ? quantity.value : 1

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

  const validSelectedIds = useMemo(() => {
    if (!product) return []
    const available = new Set(product.availableToppings.map((topping) => topping.id))
    return selectedIds.filter((id) => available.has(id))
  }, [product, selectedIds])

  const toppingsPrice = useMemo(() => {
    if (!product) return 0
    const ids = new Set(validSelectedIds)
    return product.availableToppings.filter((topping) => ids.has(topping.id)).reduce((sum, t) => sum + t.price, 0)
  }, [product, validSelectedIds])

  const groups = useMemo(() => (product ? groupToppings(product.availableToppings) : []), [product])

  function goBack() {
    if (editingLine) {
      navigate('/cart')
    } else if (location.key !== 'default') {
      navigate(-1)
    } else {
      navigate('/catalog')
    }
  }

  if (state.status === 'loading' || state.productId !== productId) {
    return (
      <section className={styles.screen}>
        <SkeletonStatus label="Загружаем товар…">
          <Skeleton height={300} radius={28} />
          <Skeleton width="60%" height={30} />
          <Skeleton width="90%" height={14} />
          <Skeleton width="70%" height={14} />
          <Skeleton width={160} height={40} radius={999} />
        </SkeletonStatus>
      </section>
    )
  }

  if (state.status === 'error' || !product) {
    return (
      <section className={styles.screen}>
        <p className={styles.alert} role="alert">
          <Icon name="alert" size={16} />
          {state.status === 'error' ? state.message : 'Товар не найден.'}
        </p>
        <Link className={styles.textLink} to="/catalog">
          Вернуться в меню
        </Link>
      </section>
    )
  }

  const unitPrice = product.basePrice + toppingsPrice
  const totalPrice = unitPrice * (editingLine ? editingLine.quantity : count)
  const favorite = isFavorite(product.id)

  function handleToggle(toppingId: number) {
    if (!product) return
    const result = toggleTopping(product.availableToppings, validSelectedIds, toppingId)
    const added = product.availableToppings.find((topping) => topping.id === toppingId)
    setSelection({
      productId,
      ids: result.ids,
      hint: result.replaced.length > 0 && added ? `${added.name} заменило: ${result.replaced.join(', ')}` : null,
    })
  }

  function handleAddToCart() {
    if (!product || !product.available) return
    if (editingLine) {
      replaceItem(editingLine.id, product, validSelectedIds)
      navigate('/cart')
      return
    }
    if (!shop) {
      navigate('/locations')
      return
    }
    const lineId = `${shop.id}:${product.id}:${[...validSelectedIds].sort((a, b) => a - b).join(',')}`
    const before = lines.find((line) => line.id === lineId)?.quantity ?? 0
    addItem(product, validSelectedIds, count, shop.id)
    // Stay in the menu (audit P1): back to where the guest came from, with an undo toast.
    showToast({
      message: count > 1 ? `${product.name} × ${count} — в корзине` : `${product.name} — в корзине`,
      action: { label: 'Отменить', onClick: () => updateQuantity(lineId, before) },
    })
    goBack()
  }

  return (
    <section className={styles.productScreen} aria-labelledby="product-title">
      <div className={styles.hero}>
        <ProductPhoto imagePath={product.imagePath} alt={product.name} className={styles.heroPhoto} />
        <div className={styles.heroControls}>
          <button className={styles.iconButton} type="button" aria-label="Назад" onClick={goBack}>
            <Icon name="left" />
          </button>
          <button
            aria-label={favorite ? 'Убрать из избранного' : 'Добавить в избранное'}
            aria-pressed={favorite}
            className={styles.iconButton}
            onClick={() => toggleFavorite(product)}
            type="button"
          >
            <Icon name="heart" />
          </button>
        </div>
      </div>

      <div className={styles.productBody}>
        <h1 className={styles.productTitle} id="product-title">
          {product.name}
        </h1>
        <p className={styles.productMeta}>
          <span>{product.categoryNameRu}</span>
          {!product.available ? (
            <>
              <span aria-hidden="true">·</span>
              <span>Нет в наличии</span>
            </>
          ) : null}
        </p>
        {product.description ? <p className={styles.productDesc}>{product.description}</p> : null}

        {groups.map((group, groupIndex) => (
          <div key={group.title}>
            <p className={styles.groupLabel} id={`topping-group-${groupIndex}`}>
              {group.title} · {group.exclusive ? 'одно' : 'любые'}
            </p>
            <div
              className={styles.chips}
              role={group.exclusive ? 'radiogroup' : 'group'}
              aria-labelledby={`topping-group-${groupIndex}`}
            >
              {group.toppings.map((topping) => {
                const selected = validSelectedIds.includes(topping.id)
                const stateProps = group.exclusive
                  ? { role: 'radio' as const, 'aria-checked': selected }
                  : { 'aria-pressed': selected }
                return (
                  <button
                    className={styles.chip}
                    key={topping.id}
                    onClick={() => handleToggle(topping.id)}
                    type="button"
                    {...stateProps}
                  >
                    {selected ? <Icon name="check" size={16} /> : null}
                    {topping.name}
                    <small>{topping.price > 0 ? `+${topping.price.toLocaleString('ru-RU')}` : '0'}</small>
                  </button>
                )
              })}
            </div>
          </div>
        ))}

        {hint ? (
          <p className={styles.hint} role="status">
            {hint}
          </p>
        ) : null}

        {!editingLine && product.available ? (
          <div className={styles.qtyRow}>
            <span>Количество</span>
            <div className={`${styles.qty} ${styles.qtySoft}`} role="group" aria-label="Количество">
              <button
                type="button"
                aria-label="Меньше"
                disabled={count <= 1}
                onClick={() => setQuantity({ productId, value: Math.max(1, count - 1) })}
              >
                <Icon name="minus" size={16} />
              </button>
              <span aria-live="polite">{count}</span>
              <button type="button" aria-label="Больше" onClick={() => setQuantity({ productId, value: count + 1 })}>
                <Icon name="plus" size={16} />
              </button>
            </div>
          </div>
        ) : null}
      </div>

      <div className={styles.buyBar}>
        <div className={styles.total}>
          <small>Итого</small>
          <b>{formatMoney(totalPrice)}</b>
        </div>
        <button
          className={`${styles.btn} ${styles.btnPrimary}`}
          onClick={handleAddToCart}
          type="button"
          disabled={!product.available}
        >
          {!product.available ? 'Нет в наличии' : editingLine ? 'Сохранить' : 'Добавить в корзину'}
        </button>
      </div>
    </section>
  )
}
