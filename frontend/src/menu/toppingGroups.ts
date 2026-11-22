import type { ToppingDto } from '../api/products'

export type ToppingGroup = {
  title: string
  /** Every topping in the group excludes the others (e.g. milk types): render as a radio group. */
  exclusive: boolean
  toppings: ToppingDto[]
}

function excludes(a: ToppingDto, b: ToppingDto) {
  return a.incompatibleWithIds.includes(b.id) || b.incompatibleWithIds.includes(a.id)
}

export function groupToppings(toppings: ToppingDto[]): ToppingGroup[] {
  const groups = new Map<string, ToppingDto[]>()
  // The API returns a Set without order: sort so groups and chips don't jump between visits.
  for (const topping of [...toppings].sort((a, b) => a.id - b.id)) {
    const title = topping.typeNameRu || 'Добавки'
    groups.set(title, [...(groups.get(title) ?? []), topping])
  }

  return Array.from(groups, ([title, items]) => ({
    title,
    toppings: items,
    exclusive: items.length > 1 && items.every((a) => items.every((b) => a.id === b.id || excludes(a, b))),
  }))
}

/**
 * Toggles a topping, dropping the ones it is incompatible with.
 * Returns the new selection and the names of toppings that were replaced, for a hint.
 */
export function toggleTopping(all: ToppingDto[], selectedIds: number[], toppingId: number) {
  const topping = all.find((entry) => entry.id === toppingId)
  if (!topping) return { ids: selectedIds, replaced: [] as string[] }

  if (selectedIds.includes(toppingId)) {
    return { ids: selectedIds.filter((id) => id !== toppingId), replaced: [] as string[] }
  }

  const removed = all.filter((entry) => selectedIds.includes(entry.id) && excludes(entry, topping))
  const removedIds = new Set(removed.map((entry) => entry.id))
  return {
    ids: [...selectedIds.filter((id) => !removedIds.has(id)), toppingId],
    replaced: removed.map((entry) => entry.name),
  }
}
