const API_ORIGIN = (import.meta.env.VITE_API_ORIGIN ?? 'http://localhost:8080').replace(/\/$/, '')
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? `${API_ORIGIN}/api`

const STORAGE_KEY = 'drinkit.auth'

export function readAccessTokenFromStorage(): string | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    return (JSON.parse(raw) as { accessToken?: string }).accessToken ?? null
  } catch {
    return null
  }
}

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export function resolveAssetUrl(path: string | null | undefined) {
  if (!path) return undefined
  if (/^https?:\/\//.test(path)) return path
  return `${API_ORIGIN}${path.startsWith('/') ? path : `/${path}`}`
}

type StoredSession = { accessToken?: string; refreshToken?: string }

function readSession(): StoredSession | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as StoredSession
  } catch {
    return null
  }
}

function readAccessToken(): string | null {
  return readSession()?.accessToken ?? null
}

function clearSessionAndRedirectToLogin() {
  localStorage.removeItem(STORAGE_KEY)
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  if (refreshPromise) return refreshPromise

  refreshPromise = (async () => {
    const session = readSession()
    if (!session?.refreshToken) return null

    try {
      const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: session.refreshToken }),
      })
      if (!response.ok) return null

      const auth = (await response.json()) as {
        accessToken: string
        refreshToken: string
        email: string
        role: string
        permissions?: string[]
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
      return auth.accessToken
    } catch {
      return null
    }
  })()

  try {
    return await refreshPromise
  } finally {
    refreshPromise = null
  }
}

async function performRequest(path: string, options: RequestInit, auth: boolean, token: string | null): Promise<Response> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  }

  if (auth && token) {
    headers.Authorization = `Bearer ${token}`
  }

  return fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })
}

export async function request<T>(path: string, options: RequestInit = {}, auth = false): Promise<T> {
  let response = await performRequest(path, options, auth, auth ? readAccessToken() : null)

  if (auth && response.status === 401) {
    const newToken = await refreshAccessToken()
    if (!newToken) {
      clearSessionAndRedirectToLogin()
      throw new ApiError(401, 'Сессия истекла, войдите снова')
    }
    response = await performRequest(path, options, auth, newToken)
  }

  const contentType = response.headers.get('content-type') ?? ''
  const body = contentType.includes('application/json') ? await response.json() : undefined

  if (!response.ok) {
    if (response.status === 401) {
      clearSessionAndRedirectToLogin()
    }
    const message = body?.error ?? body?.message ?? `Request failed with status ${response.status}`
    throw new ApiError(response.status, message)
  }

  return body as T
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>(path, { method: 'GET' }, true)
}

export function apiPost<T>(path: string, data?: unknown, auth = false): Promise<T> {
  return request<T>(
    path,
    {
      method: 'POST',
      ...(data !== undefined ? { body: JSON.stringify(data) } : {}),
    },
    auth,
  )
}

export function apiPatch<T>(path: string, data: unknown): Promise<T> {
  return request<T>(
    path,
    {
      method: 'PATCH',
      body: JSON.stringify(data),
    },
    true,
  )
}

export function apiDelete<T>(path: string): Promise<T> {
  return request<T>(path, { method: 'DELETE' }, true)
}
