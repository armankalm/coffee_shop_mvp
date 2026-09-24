import { useEffect, useMemo, useState } from 'react'

const ELAPSED_UPDATE_MS = 30_000

export function formatElapsedTime(createdAt: string, nowMs = Date.now()) {
  const createdMs = Date.parse(createdAt)

  if (Number.isNaN(createdMs)) {
    return 'нет времени'
  }

  const totalMinutes = Math.floor(Math.max(0, nowMs - createdMs) / 60_000)

  if (totalMinutes < 1) {
    return 'только что'
  }

  if (totalMinutes < 60) {
    return `${totalMinutes} мин`
  }

  const totalHours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60

  if (totalHours < 24) {
    return minutes > 0 ? `${totalHours} ч ${minutes} мин` : `${totalHours} ч`
  }

  const days = Math.floor(totalHours / 24)
  const hours = totalHours % 24

  return hours > 0 ? `${days} д ${hours} ч` : `${days} д`
}

export function useElapsedTime(createdAt: string) {
  const [nowMs, setNowMs] = useState(() => Date.now())

  useEffect(() => {
    const interval = window.setInterval(() => setNowMs(Date.now()), ELAPSED_UPDATE_MS)

    return () => window.clearInterval(interval)
  }, [createdAt])

  return useMemo(() => formatElapsedTime(createdAt, nowMs), [createdAt, nowMs])
}
