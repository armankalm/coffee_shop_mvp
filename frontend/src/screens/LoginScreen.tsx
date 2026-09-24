import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import { GOOGLE_SIGN_IN_URL, requestCode, verifyCode } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { defaultLandingPath } from '../auth/permissions'
import { Button } from '../components'
import { GoogleMark } from '../components/GoogleMark'
import styles from './Screens.module.css'
import loginStyles from './LoginScreen.module.css'

type Step = 'email' | 'code'

export function LoginScreen() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [step, setStep] = useState<Step>('email')
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [devCode, setDevCode] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const requestedPath = (location.state as { from?: Location } | null)?.from?.pathname ?? null

  function resolveRedirect(role: string) {
    // Honour an explicit destination the user was redirected away from.
    if (requestedPath) return requestedPath
    return defaultLandingPath(role)
  }

  async function handleRequestCode(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      const response = await requestCode(email)
      const nextDevCode = import.meta.env.DEV ? (response.devCode ?? null) : null
      setDevCode(nextDevCode)
      setCode(nextDevCode ?? '')
      setStep('code')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Не удалось отправить код. Попробуйте ещё раз.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleVerifyCode(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      const auth = await verifyCode(email, code)
      login(auth)
      navigate(resolveRedirect(auth.role), { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Неверный код. Попробуйте ещё раз.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className={`${styles.screen} ${loginStyles.loginScreen}`} aria-labelledby="login-title">
      <div className={loginStyles.header}>
        <p className={styles.eyebrow}>drinkit</p>
        <h1 className={styles.title} id="login-title">
          Вход по почте
        </h1>
        <p className={styles.muted}>
          {step === 'email'
            ? 'Введите email — пришлём одноразовый код для входа.'
            : `Код отправлен на ${email}. Введите 6 цифр из письма.`}
        </p>
        {devCode ? (
          <p className={loginStyles.devHint}>Dev-режим: код подставлен автоматически ({devCode})</p>
        ) : null}
      </div>

      {step === 'email' ? (
        <form className={loginStyles.form} onSubmit={handleRequestCode}>
          <label className={loginStyles.field}>
            <span className={loginStyles.fieldLabel}>Email</span>
            <input
              autoComplete="email"
              autoFocus
              className={loginStyles.input}
              inputMode="email"
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              required
              type="email"
              value={email}
            />
          </label>

          {error && <p className={loginStyles.error}>{error}</p>}

          <Button disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Отправляем...' : 'Получить код'}
          </Button>

          <p className={loginStyles.divider}>или</p>

          <Button
            className={loginStyles.googleButton}
            disabled={isSubmitting}
            onClick={() => window.location.assign(GOOGLE_SIGN_IN_URL)}
            variant="light"
          >
            <GoogleMark />
            Войти через Google
          </Button>
        </form>
      ) : (
        <form className={loginStyles.form} onSubmit={handleVerifyCode}>
          <label className={loginStyles.field}>
            <span className={loginStyles.fieldLabel}>Код из письма</span>
            <input
              autoComplete="one-time-code"
              autoFocus
              className={loginStyles.input}
              inputMode="numeric"
              maxLength={6}
              onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))}
              pattern="\d{6}"
              placeholder="000000"
              required
              type="text"
              value={code}
            />
          </label>

          {error && <p className={loginStyles.error}>{error}</p>}

          <Button disabled={isSubmitting || code.length !== 6} type="submit">
            {isSubmitting ? 'Проверяем...' : 'Войти'}
          </Button>

          <button
            className={loginStyles.linkAction}
            disabled={isSubmitting}
            onClick={() => {
              setStep('email')
              setCode('')
              setError(null)
            }}
            type="button"
          >
            Изменить email
          </button>
        </form>
      )}
    </section>
  )
}
