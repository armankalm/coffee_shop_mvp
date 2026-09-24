export type CurrencyCode = 'KZT'

export type Point = {
  id: string
  title: string
  address: string
  city: string
  readyTimeLabel: string
  distanceLabel: string
  isRecent: boolean
  isSelected?: boolean
  coordinates?: {
    lat: number
    lng: number
  }
}

export type UserProfile = {
  id: string
  name: string
  phone: string
  avatarInitials: string
  loyaltyLevel: string
}

export type Category = {
  id: string
  title: string
  subtitle: string
}

export type BadgeTone = 'blue' | 'green' | 'orange' | 'muted'

export type ProductBadge = {
  label: string
  tone: BadgeTone
}

export type Nutrition = {
  calories: number
  fats: number
  carbs: number
  proteins: number
}

export type ProductSize = {
  id: string
  label: string
  price: number
}

export type Product = {
  id: string
  categoryId: Category['id']
  title: string
  description: string
  price: number
  currency: CurrencyCode
  imageSrc: string
  imageAlt: string
  badge?: ProductBadge
  nutrition: Nutrition
  sizes: ProductSize[]
  modifierIds: Modifier['id'][]
  crossSellIds: CrossSellItem['id'][]
}

export type ModifierOption = {
  id: string
  title: string
  priceDelta: number
  selected?: boolean
}

export type Modifier = {
  id: string
  title: string
  type: 'toggle' | 'single' | 'multiple'
  options: ModifierOption[]
}

export type CrossSellItem = {
  id: string
  title: string
  price: number
  currency: CurrencyCode
  imageSrc: string
  imageAlt: string
}

export type PaymentMethod = {
  id: string
  title: string
  subtitle: string
  logoLabel: string
  selected: boolean
}

export type CartItem = {
  id: string
  productId: Product['id']
  quantity: number
  sizeId: ProductSize['id']
  modifierOptionIds: ModifierOption['id'][]
  unitPrice: number
}

export type OrderItem = {
  productId: Product['id']
  title: string
  quantity: number
  price: number
  imageSrc: string
}

export type Order = {
  id: string
  pointId: Point['id']
  createdAt: string
  status: 'completed' | 'active' | 'cancelled'
  total: number
  currency: CurrencyCode
  items: OrderItem[]
}

export type OrderPositionStatus = 'NEW' | 'IN_PROGRESS' | 'READY' | 'COMPLETED'

export type OrderPosition = {
  id: string
  orderNumber: string
  title: string
  status: OrderPositionStatus
  createdAt: string
  comment?: string
}
