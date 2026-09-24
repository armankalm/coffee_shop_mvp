import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'

import { OrderProgress } from './OrderProgress'

function currentStep(markup: string) {
  return markup.match(/aria-current="step"[^>]*>.*?<span[^>]*>([^<]+)<\/span><\/li>/)?.[1]
}

describe('OrderProgress', () => {
  it('marks the stage the order is at', () => {
    const markup = renderToStaticMarkup(<OrderProgress status="IN_PROGRESS" />)

    expect(markup).toContain('Статус заказа: Готовится')
    expect(currentStep(markup)).toBe('Готовится')
  })

  it('shows every stage done once the order is ready', () => {
    const markup = renderToStaticMarkup(<OrderProgress status="READY" />)

    expect(markup).toContain('Статус заказа: Готов')
    expect(currentStep(markup)).toBe('Готов')
  })
})
