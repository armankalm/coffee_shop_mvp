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

- [ ] Создать Entity: User (id, email, role, createdAt)
- [ ] Создать Entity: CoffeeShop (id, name, city, address, status)
- [ ] Создать Entity: Product (id, name, category, basePrice, available)
- [ ] Создать Entity: Topping (id, name, type, price, incompatibleWith)
- [ ] Создать Entity: ToppingType (MILK, SYRUP, TOPPING, EXTRAS)
- [ ] Создать Entity: Order (id, userId, shopId, status, total, createdAt)
- [ ] Создать Entity: OrderItem (id, orderId, productId, toppings, quantity, price)
- [ ] Создать Entity: SavedCombination (id, userId, productId, toppings, name)
- [ ] Создать Entity: FavoriteItem (id, userId, savedCombinationId)
- [ ] Настроить связи между таблицами (@OneToMany, @ManyToMany)
- [ ] Создать миграции Liquibase/Flyway

## Task 3: Аутентификация и авторизация

- [ ] Реализовать OTP сервис (генерация кода, отправка email)
- [ ] Создать endpoint: POST /api/auth/request-code
- [ ] Создать endpoint: POST /api/auth/verify-code
- [ ] Реализовать JWT фильтр и Token Provider
- [ ] Создать endpoint: POST /api/auth/refresh
- [ ] Настроить Spring Security с ролями
- [ ] Реализовать аннотации @PreAuthorize для методов
- [ ] Создать тесты для auth endpoints

## Task 4: Кофейни и меню

- [ ] Создать Repository: CoffeeShopRepository
- [ ] Создать endpoint: GET /api/shops (с группировкой по городу)
- [ ] Создать endpoint: GET /api/shops/{id}
- [ ] Создать endpoint: GET /api/shops/search?query=
- [ ] Создать Repository: ProductRepository
- [ ] Создать endpoint: GET /api/products (с фильтрацией по категории)
- [ ] Создать endpoint: GET /api/products/{id} (с топингами)
- [ ] Создать Repository: ToppingRepository
- [ ] Создать endpoint: GET /api/toppings (с группировкой по типу)
- [ ] Реализовать валидацию несовместимых топингов
- [ ] Создать тесты

## Task 5: Заказы и кастомизация

- [ ] Создать DTO: CreateOrderRequest (userId, shopId, items, toppings)
- [ ] Создать сервис: OrderService (создание, расчёт цены)
- [ ] Создать endpoint: POST /api/orders
- [ ] Создать endpoint: GET /api/orders (список заказов пользователя)
- [ ] Создать endpoint: GET /api/orders/{id}
- [ ] Реализовать логику расчёта цены с топингами
- [ ] Создать endpoint: POST /api/orders/{id}/cancel (отмена заказа)
- [ ] Создать SavedCombinationService (сохранение комбинаций)
- [ ] Создать endpoint: POST /api/saved-combinations
- [ ] Создать endpoint: GET /api/saved-combinations
- [ ] Создать endpoint: DELETE /api/saved-combinations/{id}
- [ ] Создать endpoint: POST /api/favorites (добавить в избранное)
- [ ] Создать endpoint: GET /api/favorites
- [ ] Создать тесты для заказов

## Task 6: Админ-панель и роли

- [ ] Создать endpoint: GET /api/admin/orders (все заказы с фильтрами)
- [ ] Создать endpoint: GET /api/admin/orders/{id}
- [ ] Создать endpoint: PATCH /api/admin/orders/{id}/status (смена статуса)
- [ ] Реализовать статусы: NEW, IN_PROGRESS, READY, COMPLETED, CANCELLED
- [ ] Создать endpoint: POST /api/admin/shops (CRUD для кофеен)
- [ ] Создать endpoint: POST /api/admin/products (CRUD для товаров)
- [ ] Создать endpoint: POST /api/admin/toppings (CRUD для топингов)
- [ ] Реализовать проверку ролей (BARISTA, MANAGER, ADMIN)
- [ ] Создать endpoint: GET /api/admin/users (список пользователей)
- [ ] Создать тесты для админ endpoints

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
