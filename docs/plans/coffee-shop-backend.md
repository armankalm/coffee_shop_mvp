# Coffee Shop System — Backend

Система управления кофейней с веб-интерфейсом для клиентов и админ-панелью для персонала.

**Стек:** Java Spring Boot + JWT + PostgreSQL  
**Архитектура:** REST API, роли (USER, BARISTA, MANAGER, ADMIN)

## Validation Commands
- `./mvnw clean test`
- `./mvnw spring-boot:run`

---

## Task 1: Настройка проекта и инфраструктуры

- [x] Инициализировать Spring Boot проект (Spring Initializr)
- [x] Настроить build систему (Maven/Gradle)
- [x] Добавить зависимости: Spring Web, Spring Data JPA, Spring Security, JWT, PostgreSQL driver
- [x] Настроить application.yml (dev/prod профили)
- [x] Настроить Docker Compose для PostgreSQL
- [x] Создать .env.example с переменными окружения
- [x] Настроить логирование (SLF4J + Logback)

## Task 2: База данных — Модели и сущности

- [x] Создать Entity: User (id, email, role, createdAt)
- [x] Создать Entity: CoffeeShop (id, name, city, address, status)
- [x] Создать Entity: Product (id, name, category, basePrice, available)
- [x] Создать Entity: Topping (id, name, type, price, incompatibleWith)
- [x] Создать Entity: ToppingType (MILK, SYRUP, TOPPING, EXTRAS)
- [x] Создать Entity: Order (id, userId, shopId, status, total, createdAt)
- [x] Создать Entity: OrderItem (id, orderId, productId, toppings, quantity, price)
- [x] Создать Entity: SavedCombination (id, userId, productId, toppings, name)
- [x] Создать Entity: FavoriteItem (id, userId, savedCombinationId)
- [x] Настроить связи между таблицами (@OneToMany, @ManyToMany)
- [x] Создать миграции Liquibase/Flyway

## Task 3: Аутентификация и авторизация

- [x] Реализовать OTP сервис (генерация кода, отправка email)
- [x] Создать endpoint: POST /api/auth/request-code
- [x] Создать endpoint: POST /api/auth/verify-code
- [x] Реализовать JWT фильтр и Token Provider
- [x] Создать endpoint: POST /api/auth/refresh
- [x] Настроить Spring Security с ролями
- [x] Реализовать аннотации @PreAuthorize для методов
- [x] Создать тесты для auth endpoints

## Task 4: Кофейни и меню

- [x] Создать Repository: CoffeeShopRepository
- [x] Создать endpoint: GET /api/shops (с группировкой по городу)
- [x] Создать endpoint: GET /api/shops/{id}
- [x] Создать endpoint: GET /api/shops/search?query=
- [x] Создать Repository: ProductRepository
- [x] Создать endpoint: GET /api/products (с фильтрацией по категории)
- [x] Создать endpoint: GET /api/products/{id} (с топингами)
- [x] Создать Repository: ToppingRepository
- [x] Создать endpoint: GET /api/toppings (с группировкой по типу)
- [x] Реализовать валидацию несовместимых топингов
- [x] Создать тесты

## Task 5: Заказы и кастомизация

- [x] Создать DTO: CreateOrderRequest (userId, shopId, items, toppings)
- [x] Создать сервис: OrderService (создание, расчёт цены)
- [x] Создать endpoint: POST /api/orders
- [x] Создать endpoint: GET /api/orders (список заказов пользователя)
- [x] Создать endpoint: GET /api/orders/{id}
- [x] Реализовать логику расчёта цены с топингами
- [x] Создать endpoint: POST /api/orders/{id}/cancel (отмена заказа)
- [x] Создать SavedCombinationService (сохранение комбинаций)
- [x] Создать endpoint: POST /api/saved-combinations
- [x] Создать endpoint: GET /api/saved-combinations
- [x] Создать endpoint: DELETE /api/saved-combinations/{id}
- [x] Создать endpoint: POST /api/favorites (добавить в избранное)
- [x] Создать endpoint: GET /api/favorites
- [x] Создать тесты для заказов

## Task 6: Админ-панель и роли

- [x] Создать endpoint: GET /api/admin/orders (все заказы с фильтрами)
- [x] Создать endpoint: GET /api/admin/orders/{id}
- [x] Создать endpoint: PATCH /api/admin/orders/{id}/status (смена статуса)
- [x] Реализовать статусы: NEW, IN_PROGRESS, READY, COMPLETED, CANCELLED
- [x] Создать endpoint: POST /api/admin/shops (CRUD для кофеен)
- [x] Создать endpoint: POST /api/admin/products (CRUD для товаров)
- [x] Создать endpoint: POST /api/admin/toppings (CRUD для топингов)
- [x] Реализовать проверку ролей (BARISTA, MANAGER, ADMIN)
- [x] Создать endpoint: GET /api/admin/users (список пользователей)
- [x] Создать тесты для админ endpoints

## Task 7: Оплата (Kaspi + Stripe)

- [ ] Создать PaymentProvider интерфейс
- [ ] Реализовать KaspiPaymentService (API Kaspi)
- [ ] Реализовать StripePaymentService
- [ ] Создать endpoint: POST /api/orders/{id}/pay
- [ ] Создать endpoint: POST /api/payments/webhook/kaspi
- [ ] Создать endpoint: POST /api/payments/webhook/stripe
- [ ] Реализовать статусы оплаты: PENDING, SUCCESS, FAILED
- [ ] Сохранять транзакции в БД
- [ ] Создать тесты для оплаты

## Task 8: Печать заказов

- [ ] Создать PrintService (интерфейс для принтеров)
- [ ] Реализовать интеграцию с термопринтером (ESC/POS)
- [ ] Создать endpoint: POST /api/admin/orders/{id}/print
- [ ] Настроить авто-печать при новом заказе (опционально)
- [ ] Создать шаблон чека (логотип, состав, штрих-код)
- [ ] Протестировать печать

## Task 9: Документация и деплой

- [ ] Настроить Swagger/OpenAPI документацию
- [ ] Создать README.md с инструкцией по запуску
- [ ] Настроить Dockerfile для приложения
- [ ] Обновить docker-compose.yml (app + db)
- [ ] Создать .env.production.example
- [ ] Настроить CI/CD (GitHub Actions)
- [ ] Финальное тестирование всех endpoints

---

## Критерии готовности (MVP)

- [ ] Пользователь может авторизоваться по email+OTP
- [ ] Пользователь может выбрать кофейню и создать заказ с топингами
- [ ] Пользователь может сохранить любимую комбинацию
- [ ] Бариста видит новые заказы и меняет статусы
- [ ] Менеджер управляет меню и кофейнями
- [ ] Оплата через Kaspi работает
- [ ] Печать заказов работает
