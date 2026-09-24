import { useEffect, useRef, useState } from 'react'

export type BoardTick = {
  /** Monotonic page counter; columns derive their page via modulo. */
  tick: number
  /** Fill ratio 0→1 of the current page's dwell time, for the progress bars. */
  progress: number
}

const PROGRESS_FPS = 20

/**
 * Drives the board's auto-rotation. Every `intervalMs` the tick increments;
 * between ticks `progress` ramps 0→1 so a progress bar can fill smoothly.
 * Only runs while `active` is true (i.e. some column overflows one page).
 */
export function useBoardTick(intervalMs: number, active: boolean): BoardTick {
  const [tick, setTick] = useState(0)
  const [progress, setProgress] = useState(0)
  const startRef = useRef(0)

  useEffect(() => {
    if (!active) {
      return
    }

    let rafActive = true
    startRef.current = performance.now()

    const frame = setInterval(() => {
      if (!rafActive) return
      const elapsed = performance.now() - startRef.current
      if (elapsed >= intervalMs) {
        startRef.current = performance.now()
        setTick((current) => current + 1)
        setProgress(0)
      } else {
        setProgress(elapsed / intervalMs)
      }
    }, 1000 / PROGRESS_FPS)

    return () => {
      rafActive = false
      clearInterval(frame)
    }
  }, [intervalMs, active])

  return { tick, progress: active ? progress : 0 }
}

export type Paged<T> = {
  items: T[]
  pageIndex: number
  pageCount: number
}

/**
 * Splits `all` into the slice visible for the given rotation `tick`.
 * Pure helper (no hooks) so it is trivially testable.
 */
export function pageForTick<T>(all: T[], pageSize: number, tick: number): Paged<T> {
  const pageCount = Math.max(1, Math.ceil(all.length / pageSize))
  const pageIndex = tick % pageCount
  const start = pageIndex * pageSize
  return {
    items: all.slice(start, start + pageSize),
    pageIndex,
    pageCount,
  }
}
