// @vitest-environment jsdom

import { afterEach, describe, expect, it, vi } from 'vitest'

import { getPushSupport } from './push'

describe('getPushSupport', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('reports no support in a browser without service workers', () => {
    expect(getPushSupport()).toBe('unsupported')
  })

  it('asks iPhone Safari users to add the site to the home screen first', () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue(
      'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Version/17.5 Mobile/15E148 Safari/604.1',
    )

    expect(getPushSupport()).toBe('needs-install')
  })
})
