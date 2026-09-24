// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

type ClientModule = typeof import('./client')

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })
}

async function loadClient(): Promise<ClientModule> {
  vi.resetModules()
  return import('./client')
}

function storeSession() {
  localStorage.setItem(
    'drinkit.auth',
    JSON.stringify({
      accessToken: 'old-access',
      refreshToken: 'refresh-token',
      email: 'user@example.com',
      role: 'USER',
    }),
  )
}

describe('api client', () => {
  beforeEach(() => {
    localStorage.clear()
    window.history.pushState({}, '', '/')
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends bearer auth headers on authenticated GET requests', async () => {
    storeSession()
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }))
    const { apiGet } = await loadClient()

    await expect(apiGet('/orders')).resolves.toEqual({ ok: true })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/orders',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer old-access',
          'Content-Type': 'application/json',
        }),
        method: 'GET',
      }),
    )
  })

  it('refreshes an expired token and retries the original request once', async () => {
    storeSession()
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ message: 'expired' }, 401))
      .mockResolvedValueOnce(
        jsonResponse({
          accessToken: 'new-access',
          refreshToken: 'new-refresh',
          email: 'user@example.com',
          role: 'USER',
        }),
      )
      .mockResolvedValueOnce(jsonResponse({ id: 10 }))
    const { apiPost } = await loadClient()

    await expect(apiPost('/orders', { item: 1 }, true)).resolves.toEqual({ id: 10 })

    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[0]?.[1]).toEqual(
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer old-access' }),
      }),
    )
    expect(fetchMock.mock.calls[2]?.[1]).toEqual(
      expect.objectContaining({
        body: JSON.stringify({ item: 1 }),
        headers: expect.objectContaining({ Authorization: 'Bearer new-access' }),
      }),
    )
    expect(JSON.parse(localStorage.getItem('drinkit.auth') ?? '{}')).toEqual(
      expect.objectContaining({ accessToken: 'new-access', refreshToken: 'new-refresh' }),
    )
  })

  it('clears the stored session when refresh fails', async () => {
    storeSession()
    window.history.pushState({}, '', '/login')
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: 'expired' }, 401))
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: 'refresh expired' }, 401))
    const { apiGet, ApiError } = await loadClient()

    await expect(apiGet('/orders')).rejects.toBeInstanceOf(ApiError)

    expect(localStorage.getItem('drinkit.auth')).toBeNull()
  })

  it('uses backend JSON error messages and non-JSON fallback messages', async () => {
    storeSession()
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: 'Bad request' }, 400))
    fetchMock.mockResolvedValueOnce(new Response('server down', { status: 500 }))
    const { apiGet } = await loadClient()

    await expect(apiGet('/bad-request')).rejects.toThrow('Bad request')
    await expect(apiGet('/server-error')).rejects.toThrow('Request failed with status 500')
  })

  it('returns undefined for successful non-JSON responses', async () => {
    storeSession()
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))
    const { apiDelete } = await loadClient()

    await expect(apiDelete('/favorite-products/10')).resolves.toBeUndefined()
  })
})
