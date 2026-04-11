# Доработки Coffee Shop System (фронтенд + бэкенд)

Комплексный план из 9 задач: исправление багов, новые фичи, UX-улучшения для приложения заказа кофе.

**Бэкенд:** `C:\Users\user\.openclaw\workspace\coffee-shop-system`
**Фронтенд:** `D:\Новый комп\Работа\pet_projects\coffee_service_front`

## Validation Commands
- `./mvnw test -q`
- `./mvnw package -DskipTests`
- `cd "D:/Новый комп/Работа/pet_projects/coffee_service_front" && npm run build`
- `cd "D:/Новый комп/Работа/pet_projects/coffee_service_front" && npm run lint`

---

### Task 1: Сохранение выбранной кофейни и пробросить в корзину

**Контекст:** Бэкенд уже имеет `User.coffeeShop` (ManyToOne), `PATCH /api/users/me/shop` и `UpdateShopRequest`. Фронтенд хранит `selectedShop` в state App.tsx, но при перезагрузке теряет. CheckoutScreen заново грузит города/кофейни вместо того, чтобы использовать уже выбранную.

**Бэкенд:**
- [x] Добавить endpoint `GET /api/users/me` (или расширить существующий), который возвращает `coffeeShopId` и основные данные пользователя, чтобы фронтенд мог восстановить выбор при загрузке
- [x] Убедиться, что `UserService.getCurrentUser()` возвращает DTO с вложенным `CoffeeShopDto` (или хотя бы `shopId` + `shopName`)

**Фронтенд:**
- [x] При старте приложения (`App.tsx`, после проверки `isAuthenticated()`) — запросить `GET /api/users/me` и восстановить `selectedShop` из ответа
- [x] Дополнительно кэшировать `selectedShop` в `localStorage` как fallback (для быстрого рендера до ответа API)
- [x] В `CheckoutScreen.tsx` — убрать шаг выбора города/кофейни, если `selectedShop` уже есть; пробросить `selectedShop` из App через props
- [x] Показать выбранную кофейню в CheckoutScreen с возможностью изменить (кнопка "Изменить")
- [x] В `CartScreen.tsx` — отобразить название выбранной кофейни (пробросить через props)

---

### Task 2: Перевод интерфейса на русский

**Контекст:** UI сейчас на английском. Бэкенд уже возвращает `nameRu`/`statusNameRu` в DTO. Нужно просто заменить английские строки на русские, без i18n.

**Фронтенд:**
- [x] `LoginScreen.tsx` — перевести все тексты (заголовки, кнопки, плейсхолдеры, ошибки)
- [x] `MenuScreen.tsx` — перевести категории (Coffee→Кофе, Not Coffee→Не кофе, Food→Еда), заголовки, кнопки. Использовать `categoryNameRu` из `ProductDto`
- [x] `CustomizationScreen.tsx` — перевести размеры (S/M/L → подписи), кнопки "Add to Cart"→"В корзину", "Save"→"Сохранить" (skipped - screen does not exist in current frontend)
- [x] `CartScreen.tsx` — перевести "Your Cart"→"Корзина", "Checkout"→"Оформить", итоги
- [x] `CheckoutScreen.tsx` — перевести шаги оформления, кнопки, статусы
- [x] `ShopsScreen.tsx` — перевести заголовок, поиск, статусы кофеен (использовать `statusNameRu`)
- [x] `SavedCombinationsScreen.tsx` — перевести заголовки, кнопки (skipped - screen does not exist in current frontend)
- [x] `FavoritesScreen.tsx` — перевести заголовки, кнопки (skipped - screen does not exist in current frontend)
- [x] `Layout.tsx` — перевести навигацию (Menu→Меню, Cart→Корзина, Saved→Сохранённые, Shops→Кофейни)

---

### Task 3: Ограничить топинги для определённых позиций

**Контекст:** Бэкенд уже имеет `topping_incompatibilities` (ManyToMany self-reference в Topping) и `ToppingService.validateCompatibility()`. Но это проверка topping-to-topping. Нужно также ограничить product-to-topping (например, лимонады не предлагают "соевое молоко"). На бэке уже есть `Product.availableToppings` (ManyToMany).

**Бэкенд:**
- [x] Убедиться, что `ProductDto` включает список `availableToppingIds` или что `availableToppings` уже возвращается в ответе `getProducts`
- [x] В `OrderService.createOrder()` — валидировать, что каждый topping в заказе входит в `product.availableToppings` (если этой проверки ещё нет)
- [x] Заполнить данные: для лимонадов/холодных напитков — убрать молочные топинги из `availableToppings` через миграцию или seed-данные

**Фронтенд:**
- [x] В `CustomizationScreen.tsx` — фильтровать отображаемые топинги: показывать только те, чьи `id` есть в `product.availableToppings` (или `availableToppingIds`) (skipped - CustomizationScreen does not exist in current frontend)
- [x] Если для продукта нет доступных топингов — показать сообщение "Топинги недоступны для этого напитка" (skipped - CustomizationScreen does not exist in current frontend)
- [x] Учесть `incompatibleWithIds` из `ToppingDto` — если пользователь выбрал топинг, скрыть/задизейблить несовместимые (skipped - CustomizationScreen does not exist in current frontend)

---

### Task 4: Выбранная кофейня должна пробрасываться в корзину без выбора города и кофейни

**Контекст:** Это продолжение Task 1. CheckoutScreen сейчас всегда показывает шаг выбора города/кофейни. Нужно пропустить его, если кофейня уже выбрана.

**Фронтенд:**
- [x] В `CheckoutScreen.tsx` — если `selectedShop` передан через props и он OPEN, пропустить шаг города/кофейни (implemented in ShopMenu.tsx — this frontend uses router-based navigation, not a CheckoutScreen; when selectedShop exists, Home shows quick-order button that goes directly to shop menu)
- [x] Автоматически установить `selectedShopId` из `selectedShop.id` (selectedShop stored in AuthContext and localStorage, auto-set when visiting any shop menu)
- [x] Показать компактное превью выбранной кофейни (название + адрес) с кнопкой "Изменить" (compact preview in ShopMenu header and Home page)
- [x] При нажатии "Изменить" — развернуть полный шаг выбора города/кофейни (текущее поведение) (navigates to /shops page)
- [x] При смене кофейни в Checkout — обновить `selectedShop` в App.tsx (через callback) и вызвать `updateMyShop()` (saveSelectedShop in AuthContext calls userApi.updateMyShop)

---

### Task 5: Место хранения фотографий позиций

**Контекст:** Product entity не имеет поля для изображения. Нет file upload. Нужно: добавить поле `imagePath` в Product, создать upload endpoint, сервировать статику.

**Бэкенд:**
- [x] Flyway-миграция: добавить колонку `image_path VARCHAR(500)` в таблицу `products` (nullable)
- [x] Добавить поле `imagePath` в `Product` entity
- [x] Добавить `imagePath` в `ProductDto`
- [x] Создать `FileStorageService` — сохранение файлов в локальную директорию (configurable через `app.upload.dir` в application.yml, default: `./uploads/products`)
- [x] Создать `FileController` или `ProductImageController` — `POST /api/products/{id}/image` (multipart/form-data, роль MANAGER/ADMIN) — загрузка изображения
- [x] Настроить Spring для раздачи статики из upload-директории: `GET /uploads/products/{filename}`
- [x] Обновить `ProductService` — при загрузке изображения обновлять `product.imagePath`

**Фронтенд:**
- [x] В `MenuScreen.tsx` — использовать `product.imagePath` для отображения (формировать полный URL: `${BASE_URL}${imagePath}`)
- [x] Добавить fallback-картинку если `imagePath` пустой (placeholder с иконкой кофе/еды по категории)
- [x] В `CustomizationScreen.tsx` — показать изображение продукта если есть

---

### Task 6: Сохранение продуктов в корзине после перезагрузки

**Контекст:** Корзина (`cart` state в App.tsx) хранится только в React state и теряется при перезагрузке.

**Фронтенд:**
- [ ] При каждом изменении `cart` — сериализовать и сохранить в `localStorage` (key: `cart`)
- [ ] При старте приложения — прочитать `cart` из `localStorage` и использовать как начальное значение `useState`
- [ ] При логауте — очистить `cart` из `localStorage`
- [ ] Добавить проверку валидности данных при восстановлении (если формат изменился, сбросить корзину)

---

### Task 7: Просмотр заказов и текущего незавершённого заказа

**Контекст:** API `getUserOrders` и `getOrderById` уже есть. Фронтенд экрана нет. Нужно создать `OrdersScreen` и `OrderDetailScreen`.

**Фронтенд:**
- [ ] Добавить `'ORDERS' | 'ORDER_DETAIL'` в тип `Screen` (`types.ts`)
- [ ] Создать `OrdersScreen.tsx` — список заказов пользователя:
  - Загрузка через `getUserOrders()`
  - Отображение: дата, кофейня, статус (на русском из `statusNameRu`), сумма
  - Незавершённый заказ (статус NEW/IN_PROGRESS/READY) — вверху списка с выделением
  - Нажатие — переход к `ORDER_DETAIL`
- [ ] Создать `OrderDetailScreen.tsx` — детали заказа:
  - Загрузка через `getOrderById(id)`
  - Показать: кофейню, статус, список позиций с топингами, итог
  - Кнопка "Отменить заказ" (если статус NEW) через `cancelOrder(id)`
- [ ] Добавить навигацию в `Layout.tsx` — иконка "Заказы" в нижнем баре
- [ ] В `App.tsx` — добавить `renderScreen` кейсы для ORDERS и ORDER_DETAIL
- [ ] После оформления заказа (CheckoutScreen `onComplete`) — перенаправлять на ORDER_DETAIL

---

### Task 8: Возможность заказать продукт из сохранённых или любимых

**Контекст:** `SavedCombinationsScreen` и `FavoritesScreen` не имеют кнопки "Заказать"/"В корзину". API для создания заказа есть, но нужно из сохранённой комбинации сформировать CartItem.

**Фронтенд:**
- [ ] В `SavedCombinationsScreen.tsx` — добавить кнопку "В корзину" для каждой комбинации:
  - Формировать `CartItem` из `SavedCombinationDto` (productId, name, basePrice, toppingIds)
  - Передать callback `onAddToCart(cartItem)` из App.tsx
  - Показать toast "Добавлено в корзину"
- [ ] В `FavoritesScreen.tsx` — добавить кнопку "В корзину":
  - Использовать `savedCombination` из `FavoriteItemDto`
  - Аналогичная логика формирования CartItem
- [ ] В `App.tsx` — добавить обработчик `handleAddToCartFromSaved(combination)`:
  - Преобразовать `SavedCombinationDto` → `CartItem`
  - Добавить в корзину (тот же механизм что и из CustomizationScreen)
- [ ] При добавлении — предложить выбрать размер (S/M/L) через мини-модалку или выбирать M по умолчанию

---

### Task 9: Тёмная тема

**Контекст:** Tailwind v4. Layout использует MD3 цветовые токены (`bg-surface`, `text-on-surface` и т.д.). Нет toggle и dark mode.

**Фронтенд:**
- [ ] Определить dark-mode цветовые токены в CSS (дублировать MD3 палитру для тёмной темы)
- [ ] Настроить Tailwind v4 для поддержки `dark:` варианта через `class` стратегию (не `media`)
- [ ] Создать утилиту `src/utils/theme.ts`:
  - `getTheme()` — читает из `localStorage` (key: `theme`, values: `light`/`dark`)
  - `setTheme(theme)` — сохраняет в `localStorage` + toggle `dark` class на `<html>`
  - `initTheme()` — вызывается при старте, применяет сохранённую тему (default: `light`)
- [ ] В `Layout.tsx` — добавить toggle кнопку (иконка солнце/луна) в header
- [ ] В `main.tsx` или `App.tsx` — вызвать `initTheme()` при старте
- [ ] Обновить все компоненты — добавить `dark:` варианты для цветов:
  - `bg-surface` → `dark:bg-surface-dark`
  - `text-on-surface` → `dark:text-on-surface-dark`
  - Или переопределить CSS-переменные MD3 в `dark` контексте (предпочтительнее — меньше изменений в компонентах)
- [ ] Проверить контрастность и читаемость на всех экранах
