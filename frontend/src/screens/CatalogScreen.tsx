import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'

import { ApiError } from '../api/client'
import type { ProductDto } from '../api/products'
import { getProducts } from '../api/products'
import { Icon, SkeletonRows } from '../components'
import { ProductPhoto } from '../components/ProductPhoto'
import { useFavorites } from '../favorites/FavoritesContext'
import { formatMoney } from '../format'
import { useQuickAdd } from '../menu/useQuickAdd'
import { useShop } from '../shop/ShopContext'
import { ShopLine } from './ShopLine'
import styles from './Cream.module.css'

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; products: ProductDto[] }

type MenuSection = { code: string; title: string; products: ProductDto[] }

/** Height of the sticky shop line + search + category tabs. */
const SPY_OFFSET_PX = 170

function lockSpy(lockedUntil: { current: number }, ms: number) {
  lockedUntil.current = Date.now() + ms
}

function sectionId(code: string) {
  return `menu-section-${code}`
}

export function MenuRow({ product }: { product: ProductDto }) {
  const { add, decrease, quantityOf } = useQuickAdd()
  const quantity = quantityOf(product.id)
  const hasOptions = product.availableToppings.length > 0
  const priceLabel = hasOptions ? `от ${formatMoney(product.basePrice)}` : formatMoney(product.basePrice)

  return (
    <article className={`${styles.item} ${product.available ? '' : styles.unavailable}`}>
      <Link className={styles.itemLink} to={`/product/${product.id}`} aria-label={`${product.name}, ${priceLabel}`}>
        <ProductPhoto className={styles.photo} imagePath={product.imagePath} width={80} />
        <span className={styles.itemBody}>
          <span className={styles.itemTitle}>{product.name}</span>
          <span className={styles.itemDesc}>
            {product.available ? (product.description ?? '') : 'Нет в наличии'}
          </span>
          <span className={styles.itemPrice}>{priceLabel}</span>
        </span>
      </Link>
      {!product.available ? (
        <span />
      ) : quantity > 0 ? (
        <div className={styles.qty} role="group" aria-label={`${product.name}: ${quantity} в корзине`}>
          <button type="button" aria-label={`Убрать один ${product.name}`} onClick={() => decrease(product)}>
            <Icon name={quantity === 1 ? 'trash' : 'minus'} size={16} />
          </button>
          <span>{quantity}</span>
          <button type="button" aria-label={`Добавить ещё ${product.name}`} onClick={() => add(product)}>
            <Icon name="plus" size={16} />
          </button>
        </div>
      ) : (
        <button
          className={`${styles.add} ${styles.addGhost}`}
          type="button"
          aria-label={hasOptions ? `Выбрать опции: ${product.name}` : `Добавить ${product.name}`}
          onClick={() => add(product)}
        >
          <Icon name="plus" />
        </button>
      )}
    </article>
  )
}

export function CatalogScreen() {
  const { shop } = useShop()
  const { favoriteProductIds } = useFavorites()

  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [query, setQuery] = useState('')
  const [onlyFavorites, setOnlyFavorites] = useState(false)
  const [activeCode, setActiveCode] = useState<string | null>(null)
  const spyLockedUntil = useRef(0)

  useEffect(() => {
    if (!shop) return
    let cancelled = false

    getProducts(shop.id)
      .then((data) => {
        if (!cancelled) setState({ status: 'ready', products: data })
      })
      .catch((err) => {
        if (!cancelled) {
          setState({ status: 'error', message: err instanceof ApiError ? err.message : 'Не удалось загрузить меню.' })
        }
      })

    return () => {
      cancelled = true
    }
  }, [shop])

  const sections = useMemo<MenuSection[]>(() => {
    if (state.status !== 'ready') return []
    const needle = query.trim().toLowerCase()
    const favorites = new Set(favoriteProductIds)

    const byCategory = new Map<string, MenuSection>()
    for (const product of state.products) {
      if (!product.available) continue
      if (onlyFavorites && !favorites.has(product.id)) continue
      if (
        needle &&
        !product.name.toLowerCase().includes(needle) &&
        !(product.description ?? '').toLowerCase().includes(needle)
      ) {
        continue
      }
      const section = byCategory.get(product.category) ?? {
        code: product.category,
        title: product.categoryNameRu,
        products: [],
      }
      section.products.push(product)
      byCategory.set(product.category, section)
    }
    return Array.from(byCategory.values())
  }, [state, query, onlyFavorites, favoriteProductIds])

  const currentCode = sections.some((section) => section.code === activeCode) ? activeCode : (sections[0]?.code ?? null)

  // Scroll-spy: highlight the last section whose top passed under the sticky header;
  // at the very bottom the last section wins (it may be too short to reach the top).
  useEffect(() => {
    if (sections.length === 0) return

    function onScroll() {
      if (Date.now() < spyLockedUntil.current) return
      const atBottom = window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 4
      let code = sections[0]!.code
      if (atBottom && window.scrollY > 0) {
        code = sections[sections.length - 1]!.code
      } else {
        for (const section of sections) {
          const top = document.getElementById(sectionId(section.code))?.getBoundingClientRect().top
          if (top !== undefined && top <= SPY_OFFSET_PX) code = section.code
        }
      }
      setActiveCode(code)
    }

    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [sections])

  if (!shop) {
    return <Navigate to="/locations" replace />
  }

  function jumpTo(code: string) {
    setActiveCode(code)
    // Don't let the smooth scroll animation re-highlight the sections it passes.
    lockSpy(spyLockedUntil, 900)
    document.getElementById(sectionId(code))?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
  }

  return (
    <section className={styles.screen} aria-labelledby="catalog-title">
      <h1 className={styles.visuallyHidden} id="catalog-title">
        Меню
      </h1>

      <div className={styles.menuTop}>
        <ShopLine shop={shop} />
        <div className={styles.searchRow}>
          <label className={styles.search}>
            <Icon name="search" />
            <input
              type="search"
              placeholder="Найти напиток"
              aria-label="Поиск по меню"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
            {query ? (
              <button className={styles.searchClear} type="button" aria-label="Очистить поиск" onClick={() => setQuery('')}>
                <Icon name="close" size={16} />
              </button>
            ) : null}
          </label>
          {favoriteProductIds.length > 0 ? (
            <button
              className={styles.iconButton}
              type="button"
              aria-label="Только избранное"
              aria-pressed={onlyFavorites}
              onClick={() => setOnlyFavorites((value) => !value)}
            >
              <Icon name="heart" />
            </button>
          ) : null}
        </div>

        {sections.length > 1 ? (
          <div className={styles.cats} role="tablist" aria-label="Категории">
            {sections.map((section) => (
              <button
                aria-controls={sectionId(section.code)}
                aria-selected={section.code === currentCode}
                className={styles.cat}
                key={section.code}
                onClick={() => jumpTo(section.code)}
                role="tab"
                type="button"
              >
                {section.title}
              </button>
            ))}
          </div>
        ) : null}
      </div>

      {state.status === 'loading' ? <SkeletonRows label="Загружаем меню…" count={5} trailing={false} /> : null}
      {state.status === 'error' ? (
        <p className={styles.alert} role="alert">
          <Icon name="alert" size={16} />
          {state.message}
        </p>
      ) : null}

      {state.status === 'ready'
        ? sections.map((section) => (
            <section
              className={styles.menuSection}
              id={sectionId(section.code)}
              data-code={section.code}
              key={section.code}
              aria-labelledby={`${sectionId(section.code)}-title`}
            >
              <h2 className={styles.menuSectionTitle} id={`${sectionId(section.code)}-title`}>
                {section.title}
              </h2>
              {section.products.map((product) => (
                <MenuRow key={product.id} product={product} />
              ))}
            </section>
          ))
        : null}

      {state.status === 'ready' && sections.length === 0 ? (
        <div className={styles.empty}>
          <span className={styles.emptyIcon}>
            <Icon name="search" size={28} />
          </span>
          <p className={styles.emptyTitle}>{query || onlyFavorites ? 'Ничего не нашлось' : 'Меню пока пустое'}</p>
          {query || onlyFavorites ? (
            <button
              className={`${styles.btn} ${styles.btnSecondary}`}
              type="button"
              onClick={() => {
                setQuery('')
                setOnlyFavorites(false)
              }}
            >
              Сбросить фильтры
            </button>
          ) : null}
        </div>
      ) : null}
    </section>
  )
}
