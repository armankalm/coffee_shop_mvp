import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import type { AuthResponse } from '../api/auth'
import { useAuth } from '../auth/AuthContext'
import { defaultLandingPath } from '../auth/permissions'
import styles from './Screens.module.css'
import loginStyles from './LoginScreen.module.css'

const ERROR_MESSAGES: Record<string, string> = {
  access_denied: 'Вы отменили вход через Google.',
  invalid_state: 'Сессия входа устарела. Попробуйте ещё раз.',
  missing_code: 'Google не передал код авторизации. Попробуйте ещё раз.',
  provider_failed: 'Google отклонил запрос. Попробуйте ещё раз.',
  provider_email_missing: 'Google не передал подтверждённый email. Войдите по коду из письма.',
  not_configured: 'Вход через Google не настроен на сервере.',
}

/** Backend redirects here with `#accessToken=...&refreshToken=...&email=...&role=...` or `#error=<code>`. */
function readCallback(): { auth: AuthResponse | null; error: string | null } {
  const params = new URLSearchParams(window.location.hash.slice(1))
  const accessToken = params.get('accessToken')
  const refreshToken = params.get('refreshToken')
  const email = params.get('email')
  const role = params.get('role')

  if (accessToken && refreshToken && email && role) {
    return { auth: { accessToken, refreshToken, email, role }, error: null }
  }

  const code = params.get('error') ?? ''
  return { auth: null, error: ERROR_MESSAGES[code] ?? 'Не удалось войти через Google.' }
}

export function OAuthCallbackScreen() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [{ auth, error }] = useState(readCallback)
  // login() gets a new identity after each session change; make sure we only log in once.
  const handled = useRef(false)

  useEffect(() => {
    if (!auth || handled.current) return
    handled.current = true
    login(auth)
    // replace: drop the tokens from the URL and from history
    navigate(defaultLandingPath(auth.role), { replace: true })
  }, [auth, login, navigate])

  return (
    <section className={`${styles.screen} ${loginStyles.loginScreen}`} aria-labelledby="oauth-title">
      <div className={loginStyles.header}>
        <p className={styles.eyebrow}>Coffee Shop</p>
        <h1 className={styles.title} id="oauth-title">
          {error ? 'Вход не выполнен' : 'Завершаем вход…'}
        </h1>
        {error && <p className={loginStyles.error}>{error}</p>}
      </div>

      {error && (
        <Link className={loginStyles.linkAction} replace to="/login">
          Вернуться ко входу
        </Link>
      )}
    </section>
  )
}
