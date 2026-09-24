import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'

import { SkeletonRows } from './Skeleton'

describe('SkeletonRows', () => {
  it('announces the label and hides the placeholders from assistive tech', () => {
    const markup = renderToStaticMarkup(<SkeletonRows label="Загружаем товары…" count={3} />)

    expect(markup).toContain('role="status"')
    expect(markup).toContain('aria-busy="true"')
    expect(markup).toContain('Загружаем товары…')
    expect(markup).toContain('aria-hidden="true"')
  })
})
