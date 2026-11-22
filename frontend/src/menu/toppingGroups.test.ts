import { describe, expect, it } from 'vitest'

import type { ToppingDto } from '../api/products'
import { groupToppings, toggleTopping } from './toppingGroups'

function topping(id: number, name: string, typeNameRu: string, incompatibleWithIds: number[] = []): ToppingDto {
  return { id, name, type: typeNameRu, typeNameRu, price: 100, incompatibleWithIds }
}

const milkCow = topping(1, 'Обычное', 'Молоко', [2])
const milkOat = topping(2, 'Овсяное', 'Молоко', [1])
const vanilla = topping(3, 'Ваниль', 'Сиропы')
const caramel = topping(4, 'Карамель', 'Сиропы')
const all = [milkCow, milkOat, vanilla, caramel]

describe('groupToppings', () => {
  it('marks mutually exclusive groups as radio groups', () => {
    const groups = groupToppings(all)

    expect(groups.map((group) => [group.title, group.exclusive])).toEqual([
      ['Молоко', true],
      ['Сиропы', false],
    ])
  })
})

describe('toggleTopping', () => {
  it('replaces an incompatible choice and reports what was replaced', () => {
    expect(toggleTopping(all, [1, 3], 2)).toEqual({ ids: [3, 2], replaced: ['Обычное'] })
  })

  it('deselects an already selected topping', () => {
    expect(toggleTopping(all, [3, 4], 3)).toEqual({ ids: [4], replaced: [] })
  })
})
