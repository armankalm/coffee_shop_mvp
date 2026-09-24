import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { getShopProducts } from '../../api/adminProducts'
import { resolveAssetUrl } from '../../api/client'
import type { ProductDto } from '../../api/products'
import { SkeletonRows } from '../../components'
import { formatMoney } from '../../mocks'
import { useStaffShops } from '../../staff/useStaffShops'
import styles from './StaffProducts.module.css'

type CategoryGroup = {
  code: string
  title: string
  products: ProductDto[]
}

function groupByCategory(products: ProductDto[]): CategoryGroup[] {
  const groups = new Map<string, CategoryGroup>()
  for (const product of products) {
    let group = groups.get(product.category)
    if (!group) {
      group = { code: product.category, title: product.categoryNameRu || product.category, products: [] }
      groups.set(product.category, group)
    }
    group.products.push(product)
  }
  return [...groups.values()]
}

export function StaffProductsScreen() {
  const { shops, selectedShopId, selectShop, loading: shopsLoading, error: shopsError } = useStaffShops()

  // Products of the shop they were loaded for; loading is derived from a shop mismatch.
  const [result, setResult] = useState<{ shopId: number; products: ProductDto[]; error: string | null } | null>(null)
  const [query, setQuery] = useState('')

  useEffect(() => {
    if (selectedShopId == null) return
    let cancelled = false

    getShopProducts(selectedShopId)
      .then((products) => {
        if (!cancelled) setResult({ shopId: selectedShopId, products, error: null })
      })
      .catch((loadError: unknown) => {
        if (!cancelled) {
          const error = loadError instanceof Error ? loadError.message : 'Не удалось загрузить товары'
          setResult({ shopId: selectedShopId, products: [], error })
        }
      })

    return () => {
      cancelled = true
    }
  }, [selectedShopId])

  const loading = selectedShopId != null && result?.shopId !== selectedShopId
  const products = useMemo(() => (loading ? [] : (result?.products ?? [])), [loading, result])
  const error = loading ? null : (result?.error ?? null)

  const groups = useMemo(() => {
    const needle = query.trim().toLowerCase()
    const filtered = needle ? products.filter((product) => product.name.toLowerCase().includes(needle)) : products
    return groupByCategory(filtered)
  }, [products, query])

  return (
    <main className={styles.screen}>
      <header className={styles.header}>
        <div className={styles.headerTitle}>
          <Link className={styles.back} to="/staff">
            ← Назад
          </Link>
          <h1 className={styles.title}>Меню</h1>
        </div>
        {selectedShopId != null ? (
          <Link className={styles.primaryLink} to={`/staff/products/new?shopId=${selectedShopId}`}>
            + Новый товар
          </Link>
        ) : null}
      </header>

      <section className={styles.toolbar}>
        <label className={styles.field}>
          <span className={styles.fieldLabel}>Кофейня</span>
          {shopsError ? (
            <p className={styles.error} role="alert">
              {shopsError}
            </p>
          ) : (
            <select
              className={styles.input}
              value={selectedShopId ?? ''}
              onChange={(event) => selectShop(Number(event.target.value))}
              disabled={shopsLoading || shops.length === 0}
            >
              {shops.map((shop) => (
                <option key={shop.id} value={shop.id}>
                  {shop.name}
                </option>
              ))}
            </select>
          )}
        </label>

        <label className={styles.field}>
          <span className={styles.fieldLabel}>Поиск</span>
          <input
            className={styles.input}
            type="search"
            placeholder="Название товара"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </label>
      </section>

      {error ? (
        <p className={styles.error} role="alert">
          {error}
        </p>
      ) : null}

      {!shopsLoading && shops.length === 0 ? <p className={styles.hint}>Вам не назначена ни одна кофейня.</p> : null}
      {loading ? <SkeletonRows label="Загружаем товары…" count={6} /> : null}
      {!loading && selectedShopId != null && groups.length === 0 && !error ? (
        <p className={styles.hint}>{query ? 'Ничего не найдено.' : 'В этой кофейне пока нет товаров.'}</p>
      ) : null}

      {!loading
        ? groups.map((group) => (
            <section key={group.code} className={styles.group}>
              <h2 className={styles.groupTitle}>{group.title}</h2>
              <ul className={styles.list}>
                {group.products.map((product) => {
                  const image = resolveAssetUrl(product.imagePath)
                  return (
                    <li key={product.id}>
                      <Link className={styles.row} to={`/staff/products/${product.id}`}>
                        {image ? (
                          <img className={styles.thumb} src={image} alt="" loading="lazy" />
                        ) : (
                          <span className={styles.thumbEmpty} aria-hidden="true">
                            ☕
                          </span>
                        )}
                        <span className={styles.rowName}>{product.name}</span>
                        {!product.available ? <span className={styles.badge}>Скрыт</span> : null}
                        <span className={styles.rowPrice}>{formatMoney(product.basePrice)}</span>
                      </Link>
                    </li>
                  )
                })}
              </ul>
            </section>
          ))
        : null}
    </main>
  )
}
