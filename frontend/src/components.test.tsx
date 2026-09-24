import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'

import {
  Badge,
  Button,
  CrossSellCard,
  HScroll,
  ListItem,
  NutritionRow,
  ProductCard,
  Stepper,
} from './components'

describe('shared UI components', () => {
  it('renders button variants and colored badges', () => {
    const markup = renderToStaticMarkup(
      <>
        <Button variant="primary">Order</Button>
        <Button variant="light">Pay</Button>
        <Button variant="fab">Near me</Button>
        <Badge tone="orange">жаз</Badge>
      </>,
    )

    expect(markup).toContain('Order')
    expect(markup).toContain('Pay')
    expect(markup).toContain('Near me')
    expect(markup).toContain('жаз')
  })

  it('renders list metadata, nutrition values, and stepper controls', () => {
    const markup = renderToStaticMarkup(
      <>
        <ListItem title="Mega Park" address="Rozybakieva 247A" time="4 min" marker />
        <NutritionRow calories={184} fats="6.5" carbs="22" proteins="7.4" />
        <Stepper value={2} min={1} max={4} />
      </>,
    )

    expect(markup).toContain('Mega Park')
    expect(markup).toContain('Rozybakieva 247A')
    expect(markup).toContain('ккал')
    expect(markup).toContain('Decrease quantity')
    expect(markup).toContain('aria-live="polite">2</span>')
  })

  it('renders commerce cards inside a hidden-scroll horizontal rail', () => {
    const markup = renderToStaticMarkup(
      <HScroll aria-label="Products">
        <ProductCard
          imageSrc="/coffee.png"
          imageAlt="Iced latte"
          title="Iced latte with vanilla protein"
          price="2 600 ₸"
          badge={{ label: 'protein 21,2 g', tone: 'green' }}
        />
        <CrossSellCard imageSrc="/porridge.png" imageAlt="Porridge" title="Oat porridge" price="1 200 ₸" />
      </HScroll>,
    )

    expect(markup).toContain('hide-scrollbar')
    expect(markup).toContain('protein 21,2 g')
    expect(markup).toContain('Iced latte with vanilla protein')
    expect(markup).toContain('Oat porridge')
    expect(markup).toContain('Add Oat porridge')
  })
})
