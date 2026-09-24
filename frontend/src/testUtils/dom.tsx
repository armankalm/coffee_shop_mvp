import { act, type ReactNode } from 'react'
import { createRoot, type Root } from 'react-dom/client'

const roots: Root[] = []

type ActGlobal = typeof globalThis & {
  IS_REACT_ACT_ENVIRONMENT?: boolean
}

;(globalThis as ActGlobal).IS_REACT_ACT_ENVIRONMENT = true

export async function renderIntoDocument(ui: ReactNode) {
  const container = document.createElement('div')
  document.body.append(container)

  const root = createRoot(container)
  roots.push(root)

  await act(async () => {
    root.render(<>{ui}</>)
  })

  return { container, root }
}

export async function clickElement(element: Element) {
  await act(async () => {
    element.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
  })
}

export async function waitFor(assertion: () => void, timeoutMs = 1000) {
  const startedAt = Date.now()
  let lastError: unknown

  while (Date.now() - startedAt < timeoutMs) {
    try {
      assertion()
      return
    } catch (error) {
      lastError = error
      await act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 10))
      })
    }
  }

  throw lastError
}

export async function cleanupDocument() {
  await act(async () => {
    for (const root of roots.splice(0)) {
      root.unmount()
    }
  })
  document.body.innerHTML = ''
}
