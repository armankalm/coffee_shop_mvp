import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import type { CoffeeShopDto } from '../api/shops'
import { getShopsGroupedByCity } from '../api/shops'
import { ApiError } from '../api/client'
import { Button, ListItem } from '../components'
import { useShop } from '../shop/ShopContext'
import styles from './Screens.module.css'

export function LocationsScreen() {
  const navigate = useNavigate()
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

  const allShops = useMemo(() => Object.values(shopsByCity ?? {}).flat(), [shopsByCity])

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

  function goToCatalog(shop: CoffeeShopDto) {
    selectShop(shop)
    navigate('/catalog')
  }

  function goNearby() {
    const shop = allShops[0]
    if (shop) goToCatalog(shop)
  }

  return (
    <section className={`${styles.screen} ${styles.locationScreen}`} aria-labelledby="locations-title">
      <h1 className={styles.visuallyHidden} id="locations-title">
        Выбор места заказа
      </h1>

      <header className={styles.locationTopBar} aria-label="Поиск точки заказа">
        <div className={styles.searchField}>
          <span className={styles.searchIcon} aria-hidden="true">
            ⌕
          </span>
          <input
            className={styles.searchInput}
            type="search"
            placeholder="Поиск"
            aria-label="Поиск"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <button className={styles.mapButton} type="button" aria-label="Открыть карту">
            <span aria-hidden="true">⌖</span>
          </button>
        </div>
        <button
          className={styles.closeButton}
          type="button"
          aria-label="Закрыть выбор адреса"
          onClick={() => selectedShop && navigate('/catalog')}
        >
          ×
        </button>
      </header>

      {isLoading ? <p className={styles.muted}>Загружаем кофейни…</p> : null}
      {error ? <p className={styles.muted}>{error}</p> : null}

      {filteredShopsByCity &&
        Object.entries(filteredShopsByCity).map(([city, shops]) => (
          <section className={styles.section} aria-labelledby={`city-${city}-title`} key={city}>
            <div className={styles.sectionHeader}>
              <h2 className={styles.sectionTitle} id={`city-${city}-title`}>
                {city}
              </h2>
            </div>
            <div className={styles.dividerList}>
              {shops.map((shop) => (
                <ListItem
                  address={shop.address}
                  key={shop.id}
                  marker={selectedShop?.id === shop.id}
                  markerLabel="Выбранная точка"
                  onClick={() => goToCatalog(shop)}
                  time={shop.statusNameRu}
                  title={shop.name}
                />
              ))}
            </div>
          </section>
        ))}

      {filteredShopsByCity && Object.keys(filteredShopsByCity).length === 0 ? (
        <p className={styles.muted}>Ничего не найдено.</p>
      ) : null}

      <div className={styles.locationFabDock}>
        <Button
          className={styles.nearbyFab}
          variant="fab"
          aria-label="Найти ближайшую точку"
          onClick={goNearby}
          disabled={allShops.length === 0}
        >
          <span aria-hidden="true">⌖</span>
          <span>Рядом со мной</span>
          <span className={styles.fabArrow} aria-hidden="true">
            →
          </span>
        </Button>
      </div>
    </section>
  )
}
