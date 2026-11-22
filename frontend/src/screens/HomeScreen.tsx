import { useEffect, useMemo, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'

import type { OrderDto } from '../api/orders'
import { getUserOrders } from '../api/orders'
import type { ProductDto } from '../api/products'
import { getProducts } from '../api/products'
import type { UserDto } from '../api/user'
import { getCurrentUser } from '../api/user'
import { Icon, Skeleton, SkeletonStatus } from '../components'
import { ProductPhoto } from '../components/ProductPhoto'
import { useFavorites } from '../favorites/FavoritesContext'
import { formatMoney, formatTime } from '../format'
import { useQuickAdd } from '../menu/useQuickAdd'
import { useRepeatOrder } from '../menu/useRepeatOrder'
import { useShop } from '../shop/ShopContext'
import { MenuRow } from './CatalogScreen'
import { ShopLine } from './ShopLine'
import styles from './Cream.module.css'

const ACTIVE_STATUSES = new Set(['NEW', 'IN_PROGRESS', 'READY'])
const ACTIVE_POLL_INTERVAL_MS = 15_000
const PROGRESS: Record<string, number> = { NEW: 18, IN_PROGRESS: 62, READY: 100 }

type HomeData = { user: UserDto | null; orders: OrderDto[]; products: ProductDto[] }

function greeting(date = new Date()) {
  const hour = date.getHours()
  if (hour >= 5 && hour < 12) return 'Доброе утро'
  if (hour >= 12 && hour < 18) return 'Добрый день'
  return 'Добрый вечер'
}

function nowTitle(order: OrderDto) {
  if (order.status === 'READY') return 'Заказ готов — можно забирать'
  if (order.status === 'IN_PROGRESS') return 'Ваш заказ готовится'
  return 'Заказ принят'
}

function relativeDay(value: string) {
  const date = new Date(value)
  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)
  if (date.toDateString() === today.toDateString()) return `Сегодня, ${formatTime(date)}`
  if (date.toDateString() === yesterday.toDateString()) return 'Вчера'
  return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' })
}

function FavoriteTile({ product }: { product: ProductDto }) {
  const { add } = useQuickAdd()
  return (
    <div className={styles.tile}>
      <div className={styles.photo}>
        <Link to={`/product/${product.id}`} aria-label={product.name} className={styles.tileLink}>
          <ProductPhoto imagePath={product.imagePath} width={132} className={styles.heroPhoto} />
        </Link>
        <button className={styles.add} type="button" aria-label={`Добавить ${product.name}`} onClick={() => add(product)}>
          <Icon name="plus" />
        </button>
      </div>
      <Link to={`/product/${product.id}`} className={styles.tileLink} tabIndex={-1}>
        <h3>{product.name}</h3>
        <span>{formatMoney(product.basePrice)}</span>
      </Link>
    </div>
  )
}

export function HomeScreen() {
  const { shop } = useShop()
  const { favoriteProducts } = useFavorites()
  const { repeat, repeatingOrderId, repeatError } = useRepeatOrder()
  const [data, setData] = useState<HomeData | null>(null)

  useEffect(() => {
    if (!shop) return
    let cancelled = false

    Promise.all([
      getCurrentUser().catch(() => null),
      getUserOrders().catch(() => [] as OrderDto[]),
      getProducts(shop.id).catch(() => [] as ProductDto[]),
    ]).then(([user, orders, products]) => {
      if (!cancelled) setData({ user, orders, products })
    })

    return () => {
      cancelled = true
    }
  }, [shop])

  const activeOrder = useMemo(
    () =>
      data?.orders
        .filter((order) => ACTIVE_STATUSES.has(order.status))
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt))[0] ?? null,
    [data],
  )

  const activeOrderId = activeOrder?.id ?? null

  // Keep the «where is my order» card live.
  useEffect(() => {
    if (activeOrderId == null) return
    let cancelled = false
    function refresh() {
      getUserOrders()
        .then((orders) => {
          if (!cancelled) setData((current) => (current ? { ...current, orders } : current))
        })
        .catch(() => undefined)
    }
    const intervalId = window.setInterval(refresh, ACTIVE_POLL_INTERVAL_MS)
    function handlePushMessage(event: MessageEvent) {
      if ((event.data as { type?: string } | null)?.type === 'push') refresh()
    }
    navigator.serviceWorker?.addEventListener('message', handlePushMessage)
    return () => {
      cancelled = true
      window.clearInterval(intervalId)
      navigator.serviceWorker?.removeEventListener('message', handlePushMessage)
    }
  }, [activeOrderId])

  if (!shop) return <Navigate to="/locations" replace />

  const lastOrder =
    data?.orders
      .filter((order) => order.status === 'COMPLETED' && order.shopId === shop.id)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))[0] ?? null
  const images = new Map((data?.products ?? []).map((product) => [product.id, product.imagePath]))
  const favorites = favoriteProducts.filter((product) => product.available)
  const popular = (data?.products ?? []).filter((product) => product.available).slice(0, 4)
  const firstName = data?.user?.name?.trim().split(/\s+/)[0]

  return (
    <section className={styles.screen} aria-labelledby="home-title">
      <ShopLine shop={shop} />
      <h1 className={styles.hello} id="home-title">
        {greeting()}
        {firstName ? (
          <>
            ,<br />
            {firstName}
          </>
        ) : null}
      </h1>

      {!data ? (
        <SkeletonStatus label="Загружаем…">
          <Skeleton height={110} radius={24} />
          <Skeleton height={72} radius={18} />
        </SkeletonStatus>
      ) : null}

      {activeOrder ? (
        <Link
          className={`${styles.now} ${activeOrder.status === 'READY' ? styles.nowReady : ''}`}
          to={`/order/${activeOrder.id}`}
        >
          <span className={styles.nowNumber}>{activeOrder.dailyNumber ?? activeOrder.id}</span>
          <p className={styles.nowTitle}>{nowTitle(activeOrder)}</p>
          <small className={styles.nowMeta}>
            {activeOrder.shopName} · {activeOrder.items.map((item) => item.productName).join(', ')}
          </small>
          <span className={styles.nowBar} aria-hidden="true">
            <i style={{ width: `${PROGRESS[activeOrder.status] ?? 0}%` }} />
          </span>
        </Link>
      ) : null}

      {lastOrder ? (
        <section className={styles.section} aria-labelledby="repeat-title">
          <div className={styles.sectionHead}>
            <h2 className={styles.sectionTitle} id="repeat-title">
              Как обычно?
            </h2>
          </div>
          <div className={styles.repeat}>
            <div className={styles.thumbs} aria-hidden="true">
              {lastOrder.items.slice(0, 3).map((item) => (
                <ProductPhoto key={item.id} className={styles.photo} imagePath={images.get(item.productId)} width={44} />
              ))}
            </div>
            <div className={styles.repeatText}>
              <p>{lastOrder.items.map((item) => item.productName).join(', ')}</p>
              <small>
                {relativeDay(lastOrder.createdAt)} · {formatMoney(lastOrder.total)}
              </small>
            </div>
            <button
              className={`${styles.btn} ${styles.btnSecondary}`}
              type="button"
              disabled={repeatingOrderId === lastOrder.id}
              onClick={() => void repeat(lastOrder)}
            >
              <Icon name="refresh" size={16} />
              Повторить
            </button>
          </div>
          {repeatError ? <p className={styles.muted}>{repeatError}</p> : null}
        </section>
      ) : null}

      {favorites.length > 0 ? (
        <section className={styles.section} aria-labelledby="favorites-title">
          <div className={styles.sectionHead}>
            <h2 className={styles.sectionTitle} id="favorites-title">
              Избранное
            </h2>
            <Link className={styles.textLink} to="/catalog">
              Всё меню
            </Link>
          </div>
          <div className={styles.rail}>
            {favorites.map((product) => (
              <FavoriteTile key={product.id} product={product} />
            ))}
          </div>
        </section>
      ) : data && popular.length > 0 ? (
        <section className={styles.section} aria-labelledby="popular-title">
          <div className={styles.sectionHead}>
            <h2 className={styles.sectionTitle} id="popular-title">
              Из меню
            </h2>
            <Link className={styles.textLink} to="/catalog">
              Всё меню
            </Link>
          </div>
          <div>
            {popular.map((product) => (
              <MenuRow key={product.id} product={product} />
            ))}
          </div>
        </section>
      ) : null}
    </section>
  )
}
