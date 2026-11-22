import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import { ApiError } from '../api/client'
import type { CoffeeShopDto } from '../api/shops'
import { getShopsGroupedByCity } from '../api/shops'
import { Icon, SkeletonRows } from '../components'
import { useShop } from '../shop/ShopContext'
import { isShopOpen } from '../shop/shopStatus'
import styles from './Cream.module.css'

export function LocationsScreen() {
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from
  const { shop: selectedShop, selectShop } = useShop()

  const [shopsByCity, setShopsByCity] = useState<Record<string, CoffeeShopDto[]> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')

  useEffect(() => {
    let cancelled = false

    getShopsGroupedByCity()
      .then((data) => {
        if (!cancelled) setShopsByCity(data)
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError ? err.message : 'Не удалось загрузить кофейни.')
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const filteredShopsByCity = useMemo(() => {
    if (!shopsByCity) return null
    const trimmed = query.trim().toLowerCase()
    if (!trimmed) return shopsByCity

    const filtered: Record<string, CoffeeShopDto[]> = {}
    for (const [city, shops] of Object.entries(shopsByCity)) {
      const matches = shops.filter(
        (shop) => shop.name.toLowerCase().includes(trimmed) || shop.address.toLowerCase().includes(trimmed),
      )
      if (matches.length > 0) filtered[city] = matches
    }
    return filtered
  }, [shopsByCity, query])

  function choose(shop: CoffeeShopDto) {
    selectShop(shop)
    navigate(from && from !== '/locations' ? from : '/home', { replace: Boolean(from) })
  }

  return (
    <section className={styles.screen} aria-labelledby="locations-title">
      <div className={styles.topBar}>
        {selectedShop ? (
          <button className={styles.iconButton} type="button" aria-label="Назад" onClick={() => navigate(from ?? '/home')}>
            <Icon name="left" />
          </button>
        ) : (
          <span aria-hidden="true" />
        )}
        <span aria-hidden="true" />
        <span aria-hidden="true" />
      </div>

      <h1 className={styles.pageTitle} id="locations-title">
        Где заберёте заказ?
      </h1>

      <label className={styles.search}>
        <Icon name="search" />
        <input
          type="search"
          placeholder="Название или адрес"
          aria-label="Поиск кофейни"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </label>

      {isLoading ? <SkeletonRows label="Загружаем кофейни…" count={4} trailing={false} /> : null}
      {error ? (
        <p className={styles.alert} role="alert">
          <Icon name="alert" size={16} />
          {error}
        </p>
      ) : null}

      {filteredShopsByCity &&
        Object.entries(filteredShopsByCity).map(([city, shops]) => (
          <section className={styles.section} aria-labelledby={`city-${shops[0]?.city.id ?? city}-title`} key={city}>
            <div className={styles.sectionHead}>
              <h2 className={styles.sectionTitle} id={`city-${shops[0]?.city.id ?? city}-title`}>
                {city}
              </h2>
            </div>
            <div>
              {shops.map((shop) => {
                const open = isShopOpen(shop)
                const selected = selectedShop?.id === shop.id
                return (
                  <button
                    className={styles.shopRow}
                    key={shop.id}
                    type="button"
                    aria-current={selected ? 'true' : undefined}
                    onClick={() => choose(shop)}
                  >
                    <span className={styles.optIcon}>
                      <Icon name={selected ? 'check' : 'pin'} />
                    </span>
                    <span className={styles.shopRowBody}>
                      <b>{shop.name}</b>
                      <span>{shop.address}</span>
                      <span className={open ? styles.statusOpen : styles.statusClosed}>
                        {open ? null : <Icon name="clock" size={14} style={{ verticalAlign: -2, marginRight: 4 }} />}
                        {shop.statusNameRu}
                      </span>
                    </span>
                    <Icon name="right" size={18} className={styles.chevron} />
                  </button>
                )
              })}
            </div>
          </section>
        ))}

      {filteredShopsByCity && Object.keys(filteredShopsByCity).length === 0 ? (
        <p className={styles.muted}>Ничего не найдено.</p>
      ) : null}
    </section>
  )
}
