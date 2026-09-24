// @vitest-environment jsdom

import { afterEach, describe, expect, it, vi } from 'vitest'

import { cleanupDocument, clickElement, renderIntoDocument } from './testUtils/dom'
import { CrossSellCard, ProductCard, Stepper } from './components'

describe('shared UI component interactions', () => {
  afterEach(async () => {
    await cleanupDocument()
  })

  it('calls stepper callbacks and respects min/max boundaries', async () => {
    const onDecrease = vi.fn()
    const onIncrease = vi.fn()
    const { container } = await renderIntoDocument(
      <>
        <Stepper value={2} min={1} max={3} onDecrease={onDecrease} onIncrease={onIncrease} />
        <Stepper value={1} min={1} onDecrease={onDecrease} />
        <Stepper value={3} max={3} onIncrease={onIncrease} />
      </>,
    )
    const buttons = Array.from(container.querySelectorAll('button'))

    await clickElement(buttons[0]!)
    await clickElement(buttons[1]!)
    await clickElement(buttons[2]!)
    await clickElement(buttons[5]!)

    expect(onDecrease).toHaveBeenCalledTimes(1)
    expect(onIncrease).toHaveBeenCalledTimes(1)
    expect((buttons[2] as HTMLButtonElement).disabled).toBe(true)
    expect((buttons[5] as HTMLButtonElement).disabled).toBe(true)
  })

  it('calls cross-sell add callback', async () => {
    const onAdd = vi.fn()
    const { container } = await renderIntoDocument(
      <CrossSellCard imageSrc="/porridge.png" imageAlt="Porridge" title="Oat porridge" price="1 200 ₸" onAdd={onAdd} />,
    )
    const addButton = container.querySelector('button[aria-label="Add Oat porridge"]')

    expect(addButton).not.toBeNull()
    await clickElement(addButton!)

    expect(onAdd).toHaveBeenCalledTimes(1)
  })

  it('keeps product open and favorite actions independent', async () => {
    const onOpen = vi.fn()
    const onFavoriteToggle = vi.fn()
    const { container } = await renderIntoDocument(
      <ProductCard
        aria-label="Open iced latte"
        imageSrc="/coffee.png"
        imageAlt="Iced latte"
        title="Iced latte"
        price="2 600 ₸"
        isFavorite={false}
        onClick={onOpen}
        onFavoriteToggle={onFavoriteToggle}
      />,
    )
    const openButton = container.querySelector('button[aria-label="Open iced latte"]')
    const favoriteButton = container.querySelector('button[aria-pressed="false"]')

    expect(openButton).not.toBeNull()
    expect(favoriteButton).not.toBeNull()

    await clickElement(favoriteButton!)
    expect(onFavoriteToggle).toHaveBeenCalledTimes(1)
    expect(onOpen).not.toHaveBeenCalled()

    await clickElement(openButton!)
    expect(onOpen).toHaveBeenCalledTimes(1)
  })
})
