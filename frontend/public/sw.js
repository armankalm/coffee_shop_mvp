// Service worker: shows Web Push notifications (e.g. "order is ready") and opens the
// order page when one is tapped. It does no caching.

self.addEventListener('install', () => {
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim())
})

self.addEventListener('push', (event) => {
  let data = {}
  try {
    data = event.data ? event.data.json() : {}
  } catch {
    data = { body: event.data ? event.data.text() : '' }
  }

  event.waitUntil(
    Promise.all([
      self.registration.showNotification(data.title || 'Coffee Shop', {
        body: data.body || '',
        icon: '/icons/icon-192.png',
        badge: '/icons/badge-96.png',
        tag: data.tag,
        renotify: Boolean(data.tag),
        data: { url: data.url || '/' },
      }),
      // Open tabs refresh right away instead of waiting for their next poll.
      self.clients
        .matchAll({ type: 'window', includeUncontrolled: true })
        .then((windows) => windows.forEach((client) => client.postMessage({ type: 'push', url: data.url }))),
    ]),
  )
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = new URL(event.notification.data?.url || '/', self.location.origin).href

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windows) => {
      const existing = windows.find((client) => client.url.startsWith(self.location.origin))
      if (existing) {
        return existing.navigate(url).then((client) => (client ?? existing).focus())
      }
      return self.clients.openWindow(url)
    }),
  )
})
