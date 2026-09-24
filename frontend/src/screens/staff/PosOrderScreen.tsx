import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { createOrder } from '../../api/orders'
import { getProducts, type ProductDto } from '../../api/products'
import { Button, Stepper } from '../../components'
import { classNames } from '../../components/classNames'
import { formatMoney } from '../../mocks'
import { useStaffShops } from '../../staff/useStaffShops'
import styles from './PosOrderScreen.module.css'

type Cart = Record<number, number> // productId -> quantity

const ALL_CATEGORIES = '__all__'

type CategoryGroup = {
  code: string
  title: string
  products: ProductDto[]
}

function groupByCategory(products: ProductDto[]): CategoryGroup[] {
  const groups = new Map<string, CategoryGroup>()
  for (const product of products) {
    const code = product.category
    let group = groups.get(code)
    if (!group) {
      group = { code, title: product.categoryNameRu || product.category || 'Прочее', products: [] }
      groups.set(code, group)
    }
    group.products.push(product)
  }
  return [...groups.values()]
}

export function PosOrderScreen() {
  const { shops, selectedShopId, selectShop, loading: shopsLoading } = useStaffShops()

  const [products, setProducts] = useState<ProductDto[]>([])
  const [productsLoading, setProductsLoading] = useState(true)
  const [customerName, setCustomerName] = useState('')
  const [cart, setCart] = useState<Cart>({})
  const [activeCategory, setActiveCategory] = useState<string>(ALL_CATEGORIES)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [createdOrderNumber, setCreatedOrderNumber] = useState<number | null>(null)

  useEffect(() => {
    if (selectedShopId == null) return

    let cancelled = false

    getProducts(selectedShopId)
      .then((list) => {
        if (cancelled) return
        setProducts(list.filter((product) => product.available))
        setCart({})
        setActiveCategory(ALL_CATEGORIES)
      })
      .catch((loadError: unknown) => {
        if (!cancelled) setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить меню')
      })
      .finally(() => {
        if (!cancelled) setProductsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [selectedShopId])

  const total = useMemo(
    () =>
      products.reduce((sum, product) => sum + (cart[product.id] ?? 0) * product.basePrice, 0),
    [products, cart],
  )

  const itemCount = useMemo(
    () => Object.values(cart).reduce((sum, quantity) => sum + quantity, 0),
    [cart],
  )

  const categories = useMemo(() => groupByCategory(products), [products])

  const visibleGroups = useMemo(
    () =>
      activeCategory === ALL_CATEGORIES
        ? categories
        : categories.filter((group) => group.code === activeCategory),
    [categories, activeCategory],
  )

  function changeQuantity(productId: number, delta: number) {
    setCart((current) => {
      const next = Math.max(0, (current[productId] ?? 0) + delta)
      const updated = { ...current, [productId]: next }
      if (next === 0) delete updated[productId]
      return updated
    })
  }

  async function submit() {
    if (selectedShopId == null || itemCount === 0 || submitting) return

    setSubmitting(true)
    setError(null)
    try {
      const trimmedName = customerName.trim()
      const order = await createOrder({
        shopId: selectedShopId,
        ...(trimmedName ? { customerName: trimmedName } : {}),
        items: Object.entries(cart).map(([productId, quantity]) => ({
          productId: Number(productId),
          toppingIds: [],
          quantity,
        })),
      })
      // Show the per-shop daily number (what the pickup board displays),
      // falling back to the global id for legacy orders.
      setCreatedOrderNumber(order.dailyNumber ?? order.id)
      setCart({})
      setCustomerName('')
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Не удалось создать заказ')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className={styles.screen}>
      <header className={styles.header}>
        <Link className={styles.back} to="/staff">
          ← Назад
        </Link>
        <h1 className={styles.title}>Новый заказ</h1>
      </header>

      {createdOrderNumber != null ? (
        <div className={styles.success} role="status">
          <p className={styles.successTitle}>Заказ №{createdOrderNumber} создан</p>
          <Button onClick={() => setCreatedOrderNumber(null)}>Создать ещё</Button>
        </div>
      ) : null}

      <section className={styles.controls}>
        <label className={styles.field}>
          <span className={styles.fieldLabel}>Имя гостя</span>
          <input
            className={styles.input}
            value={customerName}
            onChange={(event) => setCustomerName(event.target.value)}
            placeholder="Например, Роман"
            maxLength={255}
          />
        </label>

        <label className={styles.field}>
          <span className={styles.fieldLabel}>Кофейня</span>
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
        </label>
      </section>

      {error ? (
        <p className={styles.error} role="alert">
          {error}
        </p>
      ) : null}

      <section className={styles.menu} aria-label="Меню">
        {productsLoading ? (
          <p className={styles.hint}>Загрузка меню…</p>
        ) : products.length === 0 ? (
          <p className={styles.hint}>В этой кофейне нет доступных товаров.</p>
        ) : (
          <>
            {categories.length > 1 ? (
              <div className={styles.categoryTabs} role="tablist" aria-label="Категории">
                <button
                  type="button"
                  role="tab"
                  aria-selected={activeCategory === ALL_CATEGORIES}
                  className={classNames(
                    styles.categoryTab,
                    activeCategory === ALL_CATEGORIES ? styles.categoryTabActive : undefined,
                  )}
                  onClick={() => setActiveCategory(ALL_CATEGORIES)}
                >
                  Все
                </button>
                {categories.map((group) => (
                  <button
                    key={group.code}
                    type="button"
                    role="tab"
                    aria-selected={activeCategory === group.code}
                    className={classNames(
                      styles.categoryTab,
                      activeCategory === group.code ? styles.categoryTabActive : undefined,
                    )}
                    onClick={() => setActiveCategory(group.code)}
                  >
                    {group.title}
                  </button>
                ))}
              </div>
            ) : null}

            {visibleGroups.map((group) => (
              <div key={group.code} className={styles.categoryGroup}>
                <h2 className={styles.categoryTitle}>{group.title}</h2>
                <ul className={styles.list}>
                  {group.products.map((product) => (
                    <li key={product.id} className={styles.row}>
                      <span className={styles.productName}>{product.name}</span>
                      <span className={styles.productPrice}>{formatMoney(product.basePrice)}</span>
                      <Stepper
                        value={cart[product.id] ?? 0}
                        onDecrease={() => changeQuantity(product.id, -1)}
                        onIncrease={() => changeQuantity(product.id, 1)}
                        decreaseLabel={`Убрать ${product.name}`}
                        increaseLabel={`Добавить ${product.name}`}
                      />
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </>
        )}
      </section>

      <footer className={styles.footer}>
        <div className={styles.summary}>
          <span>{itemCount} поз.</span>
          <span className={styles.total}>{formatMoney(total)}</span>
        </div>
        <Button onClick={submit} disabled={itemCount === 0 || submitting || selectedShopId == null}>
          {submitting ? 'Создание…' : 'Создать заказ'}
        </Button>
      </footer>
    </main>
  )
}
