import { Link, useNavigate } from 'react-router-dom'

import { useAuth } from '../../auth/AuthContext'
import { useStaffShops } from '../../staff/useStaffShops'
import styles from './StaffHomeScreen.module.css'

export function StaffHomeScreen() {
  const { shops, selectedShopId, selectShop, loading, error } = useStaffShops()
  const { session, logout } = useAuth()
  const navigate = useNavigate()

  const boardTo = selectedShopId != null ? `/board/${selectedShopId}` : null

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <main className={styles.screen}>
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Бариста</p>
          <h1 className={styles.title}>Рабочее место</h1>
        </div>

        <div className={styles.account}>
          {session?.email ? <span className={styles.email}>{session.email}</span> : null}
          <button className={styles.logout} type="button" onClick={handleLogout}>
            Выйти
          </button>
        </div>
      </header>

      <section className={styles.shopPicker} aria-label="Кофейня">
        <label className={styles.shopLabel} htmlFor="staff-shop">
          Кофейня
        </label>
        {loading ? (
          <p className={styles.hint}>Загрузка кофеен…</p>
        ) : error ? (
          <p className={styles.error} role="alert">
            {error}
          </p>
        ) : shops.length === 0 ? (
          <p className={styles.hint}>Вам не назначена ни одна кофейня.</p>
        ) : (
          <select
            id="staff-shop"
            className={styles.select}
            value={selectedShopId ?? ''}
            onChange={(event) => selectShop(Number(event.target.value))}
          >
            {shops.map((shop) => (
              <option key={shop.id} value={shop.id}>
                {shop.name} — {shop.address}
              </option>
            ))}
          </select>
        )}
      </section>

      <nav className={styles.grid} aria-label="Разделы">
        <Link
          className={boardTo ? styles.card : styles.cardDisabled}
          to={boardTo ?? '#'}
          aria-disabled={boardTo ? undefined : true}
          onClick={(event) => {
            if (!boardTo) event.preventDefault()
          }}
        >
          <span className={styles.cardIcon} aria-hidden="true">
            📺
          </span>
          <span className={styles.cardTitle}>Табло</span>
          <span className={styles.cardText}>Экран выдачи для гостей</span>
        </Link>

        <Link className={styles.card} to="/orders">
          <span className={styles.cardIcon} aria-hidden="true">
            ☕
          </span>
          <span className={styles.cardTitle}>Кухня</span>
          <span className={styles.cardText}>Приготовление заказов</span>
        </Link>

        <Link className={styles.card} to="/staff/pos">
          <span className={styles.cardIcon} aria-hidden="true">
            ➕
          </span>
          <span className={styles.cardTitle}>Создать заказ</span>
          <span className={styles.cardText}>Оформить заказ на кассе</span>
        </Link>
      </nav>
    </main>
  )
}
