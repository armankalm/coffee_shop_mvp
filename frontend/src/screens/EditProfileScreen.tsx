import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ApiError } from '../api/client'
import { getCurrentUser, updateProfile } from '../api/user'
import { Button } from '../components'
import loginStyles from './LoginScreen.module.css'
import styles from './Screens.module.css'

export function EditProfileScreen() {
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    getCurrentUser()
      .then((user) => {
        if (cancelled) return
        setName(user.name ?? '')
        setPhone(user.phone ?? '')
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof ApiError ? err.message : 'Не удалось загрузить профиль.')
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      await updateProfile({ name, phone })
      navigate('/profile')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Не удалось сохранить профиль.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className={styles.screen} aria-labelledby="edit-profile-title">
      <header className={styles.profileHeader}>
        <Link className={styles.roundIconButton} to="/profile" aria-label="Назад">
          <span aria-hidden="true">‹</span>
        </Link>
        <h1 className={styles.title} id="edit-profile-title">
          Профиль
        </h1>
        <span className={styles.roundIconButton} aria-hidden="true" />
      </header>

      {isLoading ? (
        <p className={styles.muted}>Загружаем данные…</p>
      ) : (
        <form className={loginStyles.form} onSubmit={handleSubmit}>
          <label className={loginStyles.field}>
            <span className={loginStyles.fieldLabel}>Имя</span>
            <input
              autoFocus
              className={loginStyles.input}
              onChange={(event) => setName(event.target.value)}
              placeholder="Ваше имя"
              type="text"
              value={name}
            />
          </label>

          <label className={loginStyles.field}>
            <span className={loginStyles.fieldLabel}>Телефон</span>
            <input
              className={loginStyles.input}
              inputMode="tel"
              onChange={(event) => setPhone(event.target.value)}
              placeholder="+7 700 000 00 00"
              type="tel"
              value={phone}
            />
          </label>

          {error && <p className={loginStyles.error}>{error}</p>}

          <Button disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Сохраняем…' : 'Сохранить'}
          </Button>
        </form>
      )}
    </section>
  )
}
