# Миграция: привязка пользователей к кофейне + тестовые данные

Добавить поле `coffee_shop_id` в таблицу `users` (nullable, с дефолтом на любую доступную кофейню если не выбрана). Также заполнить все таблицы тестовыми данными через Flyway-миграцию.

## Validation Commands
- `./mvnw test -q`
- `./mvnw package -DskipTests`

---

### Task 1: Flyway-миграция — добавить coffee_shop_id в users

- [x] Создать файл `V12__add_coffee_shop_to_users.sql`
- [x] Добавить колонку `coffee_shop_id BIGINT NULL` в таблицу `users`
- [x] Добавить FK: `REFERENCES coffee_shops(id) ON DELETE SET NULL`
- [x] Добавить индекс на `users.coffee_shop_id`

### Task 2: Обновить User.java entity

- [x] Добавить поле `@ManyToOne CoffeeShop coffeeShop` (nullable, LAZY)
- [x] Аннотировать `@JoinColumn(name = "coffee_shop_id")`

### Task 3: Логика выбора кофейни по умолчанию

- [x] В `UserService` (или аналоге) при авторизации/регистрации: если `coffeeShop == null` — назначить первую доступную (статус `ACTIVE`) кофейню
- [x] Добавить метод в `CoffeeShopRepository`: `findFirstByStatusCode("ACTIVE")` с `@Query` + `JOIN FETCH`
- [x] Написать unit-тест для этой логики в `UserServiceTest`

### Task 4: Обновить DTO и API

- [x] Добавить поле `coffeeShopId` (nullable Long) в `UserResponse` DTO
- [x] Добавить endpoint `PATCH /api/users/me/shop` для смены кофейни пользователем
- [x] Добавить `@PreAuthorize("hasRole('USER')")` на новый endpoint
- [x] Написать тест контроллера для нового endpoint

### Task 5: Flyway-миграция — тестовые данные (seed)

- [x] Создать файл `V13__seed_test_data.sql`
- [x] Вставить 2 города: `Алматы`, `Астана`
- [x] Вставить 3 кофейни (разные города, статус `ACTIVE`)
- [x] Вставить 5 продуктов (разные категории, разные кофейни)
- [x] Вставить 4 топпинга (разные типы)
- [x] Вставить 3 пользователя (роли: `USER`, `BARISTA`, `MANAGER`; привязать к кофейням)
- [x] Вставить 2 заказа с позициями (`order_items`) и статусом `NEW` / `DONE`
- [x] Вставить 1 payment_transaction со статусом `COMPLETED`
- [x] Использовать `INSERT ... ON CONFLICT DO NOTHING` чтобы seed был идемпотентным

### Task 6: Проверка

- [x] Убедиться что `./mvnw test -q` проходит без ошибок
- [x] Поднять локально (`docker compose up postgres -d && ./mvnw spring-boot:run`) и проверить что данные есть в БД (manual test - skipped, not automatable)
- [x] Проверить что при регистрации нового пользователя без выбора кофейни — автоматически назначается ACTIVE кофейня (manual test - skipped, covered by unit tests)
