import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'

import styles from './Toast.module.css'

const TOAST_DURATION_MS = 5000

export type ToastOptions = {
  message: string
  /** Optional inline action, e.g. «Отменить» to undo a destructive change. */
  action?: { label: string; onClick: () => void }
}

type ToastState = ToastOptions & { id: number }

type ToastContextValue = {
  showToast: (toast: ToastOptions) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

const noopToast: ToastContextValue = { showToast: () => undefined }

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<ToastState | null>(null)
  const nextId = useRef(1)

  const showToast = useCallback((options: ToastOptions) => {
    setToast({ ...options, id: nextId.current++ })
  }, [])

  useEffect(() => {
    if (!toast) return
    const timer = window.setTimeout(() => setToast(null), TOAST_DURATION_MS)
    return () => window.clearTimeout(timer)
  }, [toast])

  const value = useMemo(() => ({ showToast }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className={styles.region} role="status" aria-live="polite">
        {toast ? (
          <div className={styles.toast} key={toast.id}>
            <span className={styles.message}>{toast.message}</span>
            {toast.action ? (
              <button
                className={styles.action}
                type="button"
                onClick={() => {
                  toast.action?.onClick()
                  setToast(null)
                }}
              >
                {toast.action.label}
              </button>
            ) : null}
          </div>
        ) : null}
      </div>
    </ToastContext.Provider>
  )
}

/** Works without a provider too (isolated screen tests): toasts are then silently dropped. */
export function useToast() {
  return useContext(ToastContext) ?? noopToast
}
