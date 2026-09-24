import drinkImage from '../assets/hero.png'
import type {
  CartItem,
  Category,
  CrossSellItem,
  Modifier,
  Order,
  OrderPosition,
  PaymentMethod,
  Point,
  Product,
  UserProfile,
} from '../types'

export const pickupPoints: Point[] = [
  {
    id: 'mega-park',
    title: 'ТРЦ Mega Park',
    address: 'ул. Розыбакиева, 247А',
    city: 'Алматы',
    readyTimeLabel: '4 мин',
    distanceLabel: '1,2 км',
    isRecent: true,
    isSelected: true,
    coordinates: { lat: 43.238, lng: 76.889 },
  },
  {
    id: 'dostyk-plaza',
    title: 'Dostyk Plaza',
    address: 'пр. Достык, 111',
    city: 'Алматы',
    readyTimeLabel: '7 мин',
    distanceLabel: '2,8 км',
    isRecent: true,
    coordinates: { lat: 43.235, lng: 76.956 },
  },
  {
    id: 'esentai-mall',
    title: 'Esentai Mall',
    address: 'пр. Аль-Фараби, 77/8',
    city: 'Алматы',
    readyTimeLabel: '9 мин',
    distanceLabel: '4,1 км',
    isRecent: false,
    coordinates: { lat: 43.219, lng: 76.928 },
  },
  {
    id: 'forum-almaty',
    title: 'Forum Almaty',
    address: 'пр. Сейфуллина, 617',
    city: 'Алматы',
    readyTimeLabel: '6 мин',
    distanceLabel: '3,5 км',
    isRecent: false,
    coordinates: { lat: 43.252, lng: 76.928 },
  },
]

export const recentPoints = pickupPoints.filter((point) => point.isRecent)
export const cityPoints = pickupPoints.filter((point) => !point.isRecent)

export const userProfile: UserProfile = {
  id: 'user-001',
  name: 'Алия Садыкова',
  phone: '+7 701 555 24 10',
  avatarInitials: 'АС',
  loyaltyLevel: 'Gold',
}

export const categories: Category[] = [
  {
    id: 'coffee',
    title: 'Кофе',
    subtitle: 'Горячие и холодные напитки',
  },
  {
    id: 'protein',
    title: 'Protein',
    subtitle: 'Напитки с повышенным белком',
  },
  {
    id: 'breakfast',
    title: 'Завтраки',
    subtitle: 'Каши, омлеты и блины',
  },
]

export const modifiers: Modifier[] = [
  {
    id: 'hot-version',
    title: 'Горячая версия',
    type: 'toggle',
    options: [{ id: 'hot', title: 'Сделать горячим', priceDelta: 0 }],
  },
  {
    id: 'sprinkles',
    title: 'Посыпки',
    type: 'multiple',
    options: [
      { id: 'cocoa', title: 'Какао', priceDelta: 100 },
      { id: 'cinnamon', title: 'Корица', priceDelta: 100 },
    ],
  },
  {
    id: 'protein-flavor',
    title: 'Ванильный протеин',
    type: 'single',
    options: [
      { id: 'vanilla-protein', title: 'Ваниль', priceDelta: 400, selected: true },
      { id: 'choco-protein', title: 'Шоколад', priceDelta: 400 },
    ],
  },
  {
    id: 'healthy-addons',
    title: 'Полезные добавки',
    type: 'multiple',
    options: [
      { id: 'chia', title: 'Чиа', priceDelta: 200 },
      { id: 'collagen', title: 'Коллаген', priceDelta: 500 },
    ],
  },
]

export const crossSellItems: CrossSellItem[] = [
  {
    id: 'oat-porridge',
    title: 'Овсяная каша',
    price: 1200,
    currency: 'KZT',
    imageSrc: drinkImage,
    imageAlt: 'Овсяная каша',
  },
  {
    id: 'cheese-omelet',
    title: 'Омлет с сыром',
    price: 1800,
    currency: 'KZT',
    imageSrc: drinkImage,
    imageAlt: 'Омлет с сыром',
  },
  {
    id: 'thin-pancakes',
    title: 'Блины',
    price: 1500,
    currency: 'KZT',
    imageSrc: drinkImage,
    imageAlt: 'Блины',
  },
]

export const featuredProduct: Product = {
  id: 'iced-latte-protein',
  categoryId: 'protein',
  title: 'Айс латте с ванильным протеином',
  description: 'Холодный латте с молоком, эспрессо и мягким ванильным протеином.',
  price: 2600,
  currency: 'KZT',
  imageSrc: drinkImage,
  imageAlt: 'Айс латте с ванильным протеином',
  badge: { label: 'protein 21,2 g', tone: 'green' },
  nutrition: {
    calories: 184,
    fats: 6.5,
    carbs: 22,
    proteins: 21.2,
  },
  sizes: [
    { id: '300', label: '300 мл', price: 2200 },
    { id: '400', label: '400 мл', price: 2600 },
  ],
  modifierIds: ['hot-version', 'sprinkles', 'protein-flavor', 'healthy-addons'],
  crossSellIds: ['oat-porridge', 'cheese-omelet'],
}

export const products: Product[] = [
  featuredProduct,
  {
    id: 'flat-white',
    categoryId: 'coffee',
    title: 'Флэт уайт',
    description: 'Двойной эспрессо и плотная молочная текстура.',
    price: 1700,
    currency: 'KZT',
    imageSrc: drinkImage,
    imageAlt: 'Флэт уайт',
    badge: { label: 'decaf', tone: 'blue' },
    nutrition: {
      calories: 118,
      fats: 4.7,
      carbs: 9.4,
      proteins: 6.2,
    },
    sizes: [
      { id: '250', label: '250 мл', price: 1500 },
      { id: '350', label: '350 мл', price: 1700 },
    ],
    modifierIds: ['sprinkles'],
    crossSellIds: ['thin-pancakes'],
  },
  {
    id: 'orange-matcha',
    categoryId: 'coffee',
    title: 'Матча апельсин',
    description: 'Матча, апельсиновый фреш и лед.',
    price: 2300,
    currency: 'KZT',
    imageSrc: drinkImage,
    imageAlt: 'Матча апельсин',
    badge: { label: 'жаз', tone: 'orange' },
    nutrition: {
      calories: 142,
      fats: 2.1,
      carbs: 26,
      proteins: 3.8,
    },
    sizes: [
      { id: '350', label: '350 мл', price: 2100 },
      { id: '450', label: '450 мл', price: 2300 },
    ],
    modifierIds: ['healthy-addons'],
    crossSellIds: ['oat-porridge'],
  },
  {
    id: 'oatmeal-bowl',
    categoryId: 'breakfast',
    title: 'Oatmeal with berries',
    description: 'Oatmeal with milk, berries, and honey.',
    price: 1200,
    currency: 'KZT',
    imageSrc: drinkImage,
    imageAlt: 'Oatmeal with berries',
    nutrition: {
      calories: 212,
      fats: 5.8,
      carbs: 34,
      proteins: 7.6,
    },
    sizes: [{ id: 'bowl', label: '1 portion', price: 1200 }],
    modifierIds: ['healthy-addons'],
    crossSellIds: ['thin-pancakes'],
  },
]

export const paymentMethods: PaymentMethod[] = [
  {
    id: 'kaspi',
    title: 'Kaspi.kz',
    subtitle: 'Оплата в приложении',
    logoLabel: 'Kaspi',
    selected: true,
  },
  {
    id: 'card',
    title: 'Банковская карта',
    subtitle: 'Visa или Mastercard',
    logoLabel: 'Card',
    selected: false,
  },
]

export const cartItems: CartItem[] = [
  {
    id: 'cart-iced-latte-protein',
    productId: featuredProduct.id,
    quantity: 1,
    sizeId: '400',
    modifierOptionIds: ['vanilla-protein'],
    unitPrice: featuredProduct.price,
  },
]

export const orderHistory: Order[] = [
  {
    id: 'order-1204',
    pointId: 'mega-park',
    createdAt: '2026-07-08T08:20:00+05:00',
    status: 'completed',
    total: 3800,
    currency: 'KZT',
    items: [
      {
        productId: featuredProduct.id,
        title: featuredProduct.title,
        quantity: 1,
        price: featuredProduct.price,
        imageSrc: featuredProduct.imageSrc,
      },
      {
        productId: 'oat-porridge',
        title: 'Овсяная каша',
        quantity: 1,
        price: 1200,
        imageSrc: drinkImage,
      },
    ],
  },
  {
    id: 'order-1191',
    pointId: 'dostyk-plaza',
    createdAt: '2026-07-04T12:05:00+05:00',
    status: 'completed',
    total: 3200,
    currency: 'KZT',
    items: [
      {
        productId: 'flat-white',
        title: 'Флэт уайт',
        quantity: 1,
        price: 1700,
        imageSrc: drinkImage,
      },
      {
        productId: 'thin-pancakes',
        title: 'Блины',
        quantity: 1,
        price: 1500,
        imageSrc: drinkImage,
      },
    ],
  },
]

export const orderPositions: OrderPosition[] = [
  {
    id: 'position-1208-1',
    orderNumber: '1208',
    title: 'Iced latte 400 ml',
    status: 'NEW',
    createdAt: '2026-07-19T09:05:00+05:00',
    comment: 'Oat milk',
  },
  {
    id: 'position-1208-2',
    orderNumber: '1208',
    title: 'Oatmeal with berries',
    status: 'NEW',
    createdAt: '2026-07-19T09:06:00+05:00',
  },
  {
    id: 'position-1209-1',
    orderNumber: '1209',
    title: 'Flat white 350 ml',
    status: 'NEW',
    createdAt: '2026-07-19T09:08:00+05:00',
    comment: 'Extra hot',
  },
  {
    id: 'position-1210-1',
    orderNumber: '1210',
    title: 'Orange matcha 450 ml',
    status: 'IN_PROGRESS',
    createdAt: '2026-07-19T08:58:00+05:00',
  },
  {
    id: 'position-1210-2',
    orderNumber: '1210',
    title: 'Thin pancakes',
    status: 'IN_PROGRESS',
    createdAt: '2026-07-19T08:59:00+05:00',
    comment: 'No honey',
  },
  {
    id: 'position-1211-1',
    orderNumber: '1211',
    title: 'Cappuccino 300 ml',
    status: 'IN_PROGRESS',
    createdAt: '2026-07-19T09:02:00+05:00',
  },
  {
    id: 'position-1212-1',
    orderNumber: '1212',
    title: 'Americano 300 ml',
    status: 'READY',
    createdAt: '2026-07-19T08:50:00+05:00',
    comment: 'No sugar',
  },
  {
    id: 'position-1213-1',
    orderNumber: '1213',
    title: 'Latte 350 ml',
    status: 'READY',
    createdAt: '2026-07-19T08:54:00+05:00',
  },
  {
    id: 'position-1214-1',
    orderNumber: '1214',
    title: 'Cheese omelet',
    status: 'COMPLETED',
    createdAt: '2026-07-19T08:40:00+05:00',
  },
  {
    id: 'position-1215-1',
    orderNumber: '1215',
    title: 'Protein iced latte 400 ml',
    status: 'NEW',
    createdAt: '2026-07-19T09:10:00+05:00',
    comment: 'Vanilla protein',
  },
]

export function formatMoney(amount: number) {
  return `${amount.toLocaleString('ru-RU')} ₸`
}
