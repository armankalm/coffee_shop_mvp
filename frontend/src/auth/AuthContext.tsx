import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

import type { AuthResponse } from '../api/auth'

const STORAGE_KEY = 'drinkit.auth'

export type AuthSession = {
  accessToken: string
  refreshToken: string
  email: string
  role: string
  permissions?: string[]
}

type AuthContextValue = {
  session: AuthSession | null
  login: (auth: AuthResponse) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every(isNonEmptyString)
}

function isAuthSession(value: unknown): value is AuthSession {
  if (typeof value !== 'object' || value === null) return false

  const candidate = value as Partial<AuthSession>

  return (
    isNonEmptyString(candidate.accessToken) &&
    isNonEmptyString(candidate.refreshToken) &&
    isNonEmptyString(candidate.email) &&
    isNonEmptyString(candidate.role) &&
    (candidate.permissions === undefined || isStringArray(candidate.permissions))
  )
}

function readStoredSession(): AuthSession | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as unknown
    return isAuthSession(parsed) ? parsed : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => readStoredSession())

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      login: (auth) => {
        const nextSession: AuthSession = {
          accessToken: auth.accessToken,
          refreshToken: auth.refreshToken,
          email: auth.email,
          role: auth.role,
        }
        if (auth.permissions) {
          nextSession.permissions = auth.permissions
        }
        localStorage.setItem(STORAGE_KEY, JSON.stringify(nextSession))
        setSession(nextSession)
      },
      logout: () => {
        localStorage.removeItem(STORAGE_KEY)
        setSession(null)
      },
    }),
    [session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
