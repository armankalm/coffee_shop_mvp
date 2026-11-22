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

/** SLA bands of the kitchen board (audit B3): < 3 min fresh, 3–6 min warn, > 6 min late. */
export type SlaLevel = 'fresh' | 'warn' | 'late'

export const SLA_WARN_MS = 3 * 60_000
export const SLA_LATE_MS = 6 * 60_000

export function slaLevel(elapsedMs: number): SlaLevel {
  if (elapsedMs >= SLA_LATE_MS) return 'late'
  if (elapsedMs >= SLA_WARN_MS) return 'warn'
  return 'fresh'
}

/** Stopwatch label: «4:07», «1:05:30». */
export function formatTimer(elapsedMs: number) {
  const totalSeconds = Math.floor(Math.max(0, elapsedMs) / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return hours > 0 ? `${hours}:${String(minutes).padStart(2, '0')}:${seconds}` : `${minutes}:${seconds}`
}

/** Milliseconds since `createdAt`, re-rendering every second. */
export function useElapsedMs(createdAt: string) {
  const [nowMs, setNowMs] = useState(() => Date.now())

  useEffect(() => {
    const interval = window.setInterval(() => setNowMs(Date.now()), 1000)
    return () => window.clearInterval(interval)
  }, [])

  const createdMs = Date.parse(createdAt)
  return Number.isNaN(createdMs) ? 0 : Math.max(0, nowMs - createdMs)
}

export function useElapsedTime(createdAt: string) {
  const [nowMs, setNowMs] = useState(() => Date.now())

  useEffect(() => {
    const interval = window.setInterval(() => setNowMs(Date.now()), ELAPSED_UPDATE_MS)

    return () => window.clearInterval(interval)
  }, [createdAt])

  return useMemo(() => formatElapsedTime(createdAt, nowMs), [createdAt, nowMs])
}
