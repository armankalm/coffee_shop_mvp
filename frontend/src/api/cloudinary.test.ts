import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('cloudinary', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubEnv('VITE_CLOUDINARY_CLOUD_NAME', 'demo-cloud')
    vi.stubEnv('VITE_CLOUDINARY_UPLOAD_PRESET', 'demo-preset')
  })

  afterEach(() => {
    vi.unstubAllEnvs()
    vi.unstubAllGlobals()
  })

  it('uploads each shop into its own folder', async () => {
    const fetchMock = vi.fn().mockImplementation(
      async () =>
        new Response(
          JSON.stringify({ secure_url: 'https://res.cloudinary.com/demo-cloud/image/upload/v1/x.jpg', public_id: 'x' }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ),
    )
    vi.stubGlobal('fetch', fetchMock)
    const { uploadToCloudinary } = await import('./cloudinary')

    await uploadToCloudinary(new File(['img'], 'a.jpg', { type: 'image/jpeg' }), { shopId: 7, productId: 42 })
    await uploadToCloudinary(new File(['img'], 'b.jpg', { type: 'image/jpeg' }), { shopId: 9, productId: 43 })

    const [firstUrl, firstInit] = fetchMock.mock.calls[0] as [string, RequestInit]
    const [, secondInit] = fetchMock.mock.calls[1] as [string, RequestInit]
    expect(firstUrl).toBe('https://api.cloudinary.com/v1_1/demo-cloud/image/upload')
    expect((firstInit.body as FormData).get('folder')).toBe('coffee-mvp/shop-7/products')
    expect((firstInit.body as FormData).get('upload_preset')).toBe('demo-preset')
    expect((secondInit.body as FormData).get('folder')).toBe('coffee-mvp/shop-9/products')
  })

  it('surfaces the Cloudinary error message', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ error: { message: 'Upload preset not found' } }), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
    const { uploadToCloudinary } = await import('./cloudinary')

    await expect(
      uploadToCloudinary(new File(['img'], 'a.jpg', { type: 'image/jpeg' }), { shopId: 1, productId: 1 }),
    ).rejects.toThrow('Upload preset not found')
  })

  it('adds resize transforms only to Cloudinary URLs', async () => {
    const { cloudinaryThumb } = await import('./cloudinary')

    expect(cloudinaryThumb('https://res.cloudinary.com/c/image/upload/v1/a.jpg', 160)).toBe(
      'https://res.cloudinary.com/c/image/upload/c_fill,w_160,h_160,q_auto,f_auto/v1/a.jpg',
    )
    expect(cloudinaryThumb('http://localhost:8080/uploads/products/a.jpg', 160)).toBe(
      'http://localhost:8080/uploads/products/a.jpg',
    )
  })
})
