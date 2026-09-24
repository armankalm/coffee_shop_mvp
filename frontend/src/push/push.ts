import { apiGet, apiPost } from '../api/client'

const SERVICE_WORKER_URL = '/sw.js'

export type PushConfig = { enabled: boolean; publicKey: string }

/**
 * - `supported`: this browser can subscribe right away.
 * - `needs-install`: iPhone/iPad Safari, where Web Push only works once the site is
 *   added to the home screen and opened from there.
 * - `unsupported`: no Web Push at all.
 */
export type PushSupport = 'supported' | 'needs-install' | 'unsupported'

export function getPushSupport(): PushSupport {
  if ('serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window) {
    return 'supported'
  }
  const isAppleMobile =
    /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  const isStandalone =
    window.matchMedia?.('(display-mode: standalone)')?.matches === true ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  return isAppleMobile && !isStandalone ? 'needs-install' : 'unsupported'
}

export function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return
  navigator.serviceWorker.register(SERVICE_WORKER_URL).catch(() => {
    // Push is optional; the app works without the worker.
  })
}

export function getPushConfig() {
  return apiGet<PushConfig>('/push/config')
}

async function currentSubscription() {
  const registration = await navigator.serviceWorker.getRegistration(SERVICE_WORKER_URL)
  return (await registration?.pushManager.getSubscription()) ?? null
}

/** Whether this browser already receives pushes; re-links the subscription to the current user. */
export async function syncExistingSubscription(): Promise<boolean> {
  if (getPushSupport() !== 'supported' || Notification.permission !== 'granted') return false
  const subscription = await currentSubscription()
  if (!subscription) return false
  await apiPost('/push/subscriptions', subscription.toJSON(), true)
  return true
}

function base64UrlToBytes(value: string) {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=')
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index)
  return bytes
}

/**
 * Must be called from a click handler: Safari only shows the permission prompt for a
 * user gesture, so the permission is requested before any network round trip.
 */
export async function enablePush(publicKey: string): Promise<'enabled' | 'denied'> {
  const permission = await Notification.requestPermission()
  if (permission !== 'granted') return 'denied'

  const registration = await navigator.serviceWorker.register(SERVICE_WORKER_URL)
  await navigator.serviceWorker.ready
  const options = { userVisibleOnly: true, applicationServerKey: base64UrlToBytes(publicKey) }

  let subscription: PushSubscription
  try {
    subscription = await registration.pushManager.subscribe(options)
  } catch (error) {
    // An old subscription made with a different server key blocks a new one: replace it.
    const existing = await registration.pushManager.getSubscription()
    if (!existing) throw error
    await existing.unsubscribe()
    subscription = await registration.pushManager.subscribe(options)
  }

  await apiPost('/push/subscriptions', subscription.toJSON(), true)
  return 'enabled'
}

/** Stops pushes on this browser, e.g. on logout so the next account doesn't get them. */
export async function disablePush() {
  if (getPushSupport() !== 'supported') return
  const subscription = await currentSubscription()
  if (!subscription) return
  // Unsubscribing is enough: the push service then answers 410 and the server drops the row.
  await subscription.unsubscribe()
}
