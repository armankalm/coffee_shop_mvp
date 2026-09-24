import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'

import {
  createProduct,
  deleteProduct,
  getAllToppings,
  getProductCategories,
  updateProduct,
  uploadProductImage,
  type ProductCategoryDto,
  type ProductInput,
} from '../../api/adminProducts'
import { resolveAssetUrl } from '../../api/client'
import { getProductById, type ToppingDto } from '../../api/products'
import { SkeletonForm } from '../../components'
import styles from './StaffProducts.module.css'

const ACCEPTED_IMAGES = 'image/jpeg,image/png,image/webp,image/gif'
const MAX_IMAGE_BYTES = 5 * 1024 * 1024

type FormState = {
  name: string
  categoryCode: string
  price: string
  description: string
  available: boolean
  toppingIds: Set<number>
}

const EMPTY_FORM: FormState = {
  name: '',
  categoryCode: '',
  price: '',
  description: '',
  available: true,
  toppingIds: new Set(),
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}

/** Create (/staff/products/new?shopId=) or edit (/staff/products/:productId) a product. */
export function StaffProductEditScreen() {
  const { productId: productIdParam } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  const productId = productIdParam ? Number(productIdParam) : null
  const isNew = productId == null

  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [shopId, setShopId] = useState<number | null>(() => {
    const fromQuery = Number(searchParams.get('shopId'))
    return isNew && Number.isFinite(fromQuery) && fromQuery > 0 ? fromQuery : null
  })
  const [imagePath, setImagePath] = useState<string | null>(null)
  const [pendingImage, setPendingImage] = useState<File | null>(null)
  const [categories, setCategories] = useState<ProductCategoryDto[]>([])
  const [toppings, setToppings] = useState<ToppingDto[]>([])

  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [error, setError] = useState<string | null>(() =>
    searchParams.get('photoFailed') ? 'Товар создан, но фото не загрузилось. Попробуйте ещё раз.' : null,
  )
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    Promise.all([getProductCategories(), getAllToppings(), productId != null ? getProductById(productId) : null])
      .then(([loadedCategories, loadedToppings, product]) => {
        if (cancelled) return
        setCategories(loadedCategories)
        setToppings(loadedToppings)
        if (product) {
          setForm({
            name: product.name,
            categoryCode: product.category,
            price: String(product.basePrice),
            description: product.description ?? '',
            available: product.available,
            toppingIds: new Set(product.availableToppings.map((topping) => topping.id)),
          })
          setShopId(product.shopId ?? null)
          setImagePath(product.imagePath)
        } else {
          setForm((current) => ({ ...current, categoryCode: loadedCategories[0]?.code ?? '' }))
        }
      })
      .catch((loadError: unknown) => {
        if (!cancelled) setError(errorMessage(loadError, 'Не удалось загрузить товар'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [productId])

  const pendingPreview = useMemo(() => (pendingImage ? URL.createObjectURL(pendingImage) : null), [pendingImage])
  useEffect(() => {
    return () => {
      if (pendingPreview) URL.revokeObjectURL(pendingPreview)
    }
  }, [pendingPreview])

  const preview = pendingPreview ?? resolveAssetUrl(imagePath)

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  function toggleTopping(toppingId: number) {
    setForm((current) => {
      const next = new Set(current.toppingIds)
      if (next.has(toppingId)) next.delete(toppingId)
      else next.add(toppingId)
      return { ...current, toppingIds: next }
    })
  }

  async function handleImageChange(file: File | undefined) {
    setError(null)
    setNotice(null)
    if (!file) return
    if (file.size > MAX_IMAGE_BYTES) {
      setError('Файл больше 5 МБ')
      return
    }

    if (productId == null) {
      // A new product has no id yet: upload right after it is created.
      setPendingImage(file)
      return
    }

    setUploading(true)
    try {
      const updated = await uploadProductImage(productId, file)
      setImagePath(updated.imagePath)
      setNotice('Фото обновлено')
    } catch (uploadError) {
      setError(errorMessage(uploadError, 'Не удалось загрузить фото'))
    } finally {
      setUploading(false)
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setNotice(null)

    const price = Number(form.price.replace(',', '.'))
    if (!form.name.trim()) {
      setError('Укажите название')
      return
    }
    if (!Number.isFinite(price) || price <= 0) {
      setError('Цена должна быть больше нуля')
      return
    }
    if (!form.categoryCode) {
      setError('Выберите категорию')
      return
    }
    if (shopId == null) {
      setError('Не выбрана кофейня')
      return
    }

    const input: ProductInput = {
      shopId,
      name: form.name.trim(),
      categoryCode: form.categoryCode,
      basePrice: price,
      available: form.available,
      description: form.description.trim() || null,
      availableToppingIds: [...form.toppingIds],
    }

    setSaving(true)
    try {
      if (productId == null) {
        const created = await createProduct(input)
        if (pendingImage) {
          try {
            await uploadProductImage(created.id, pendingImage)
          } catch {
            // The product exists now; let the user retry the photo on its edit page.
            navigate(`/staff/products/${created.id}?photoFailed=1`, { replace: true })
            return
          }
        }
      } else {
        await updateProduct(productId, input)
      }
      navigate('/staff/products')
    } catch (saveError) {
      setError(errorMessage(saveError, 'Не удалось сохранить товар'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (productId == null) return
    setError(null)
    setSaving(true)
    try {
      await deleteProduct(productId)
      navigate('/staff/products')
    } catch (deleteError) {
      setError(errorMessage(deleteError, 'Не удалось удалить товар'))
      setSaving(false)
    }
  }

  return (
    <main className={styles.screen}>
      <header className={styles.header}>
        <div className={styles.headerTitle}>
          <Link className={styles.back} to="/staff/products">
            ← Меню
          </Link>
          <h1 className={styles.title}>{isNew ? 'Новый товар' : 'Товар'}</h1>
        </div>
      </header>

      {loading ? <SkeletonForm label="Загружаем товар…" fields={4} image /> : null}

      {!loading ? (
        <form className={styles.form} onSubmit={handleSubmit} noValidate>
          <section className={styles.imageBlock}>
            {preview ? (
              <img className={styles.imagePreview} src={preview} alt="" />
            ) : (
              <span className={styles.imageEmpty} aria-hidden="true">
                ☕
              </span>
            )}
            <label className={styles.secondaryButton}>
              {uploading ? 'Загружаем…' : preview ? 'Заменить фото' : 'Загрузить фото'}
              <input
                className={styles.fileInput}
                type="file"
                accept={ACCEPTED_IMAGES}
                disabled={uploading || saving}
                onChange={(event) => {
                  void handleImageChange(event.target.files?.[0])
                  event.target.value = ''
                }}
              />
            </label>
            <span className={styles.hint}>JPEG, PNG, WebP или GIF до 5 МБ</span>
          </section>

          <label className={styles.field}>
            <span className={styles.fieldLabel}>Название</span>
            <input
              className={styles.input}
              value={form.name}
              onChange={(event) => update('name', event.target.value)}
              maxLength={255}
            />
          </label>

          <div className={styles.twoColumns}>
            <label className={styles.field}>
              <span className={styles.fieldLabel}>Категория</span>
              <select
                className={styles.input}
                value={form.categoryCode}
                onChange={(event) => update('categoryCode', event.target.value)}
              >
                {categories.map((category) => (
                  <option key={category.code} value={category.code}>
                    {category.nameRu}
                  </option>
                ))}
              </select>
            </label>

            <label className={styles.field}>
              <span className={styles.fieldLabel}>Цена, ₸</span>
              <input
                className={styles.input}
                inputMode="decimal"
                value={form.price}
                onChange={(event) => update('price', event.target.value)}
              />
            </label>
          </div>

          <label className={styles.field}>
            <span className={styles.fieldLabel}>Описание</span>
            <textarea
              className={`${styles.input} ${styles.textarea}`}
              value={form.description}
              onChange={(event) => update('description', event.target.value)}
              rows={3}
            />
          </label>

          <label className={styles.checkRow}>
            <input
              type="checkbox"
              checked={form.available}
              onChange={(event) => update('available', event.target.checked)}
            />
            <span>
              В продаже
              <span className={styles.checkHint}>Снимите, чтобы временно скрыть товар из меню</span>
            </span>
          </label>

          {toppings.length > 0 ? (
            <fieldset className={styles.fieldset}>
              <legend className={styles.fieldLabel}>Доступные добавки</legend>
              {toppings.map((topping) => (
                <label key={topping.id} className={styles.checkRow}>
                  <input
                    type="checkbox"
                    checked={form.toppingIds.has(topping.id)}
                    onChange={() => toggleTopping(topping.id)}
                  />
                  <span>
                    {topping.name}
                    <span className={styles.checkHint}>
                      {topping.typeNameRu} · +{topping.price} ₸
                    </span>
                  </span>
                </label>
              ))}
            </fieldset>
          ) : null}

          {error ? (
            <p className={styles.error} role="alert">
              {error}
            </p>
          ) : null}
          {notice ? <p className={styles.notice}>{notice}</p> : null}

          <div className={styles.actions}>
            <button className={styles.primaryButton} type="submit" disabled={saving || uploading}>
              {saving ? 'Сохраняем…' : isNew ? 'Создать' : 'Сохранить'}
            </button>

            {!isNew ? (
              confirmDelete ? (
                <div className={styles.confirm}>
                  <span>Удалить товар из меню?</span>
                  <button className={styles.dangerButton} type="button" onClick={handleDelete} disabled={saving}>
                    Да, удалить
                  </button>
                  <button className={styles.secondaryButton} type="button" onClick={() => setConfirmDelete(false)}>
                    Отмена
                  </button>
                </div>
              ) : (
                <button className={styles.dangerOutline} type="button" onClick={() => setConfirmDelete(true)}>
                  Удалить
                </button>
              )
            ) : null}
          </div>
        </form>
      ) : null}
    </main>
  )
}
