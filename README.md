# Coffee Shop System

Система управления кофейней с REST API на Spring Boot.

## Возможности

- Аутентификация по email + OTP (одноразовый код)
- JWT токены (access + refresh)
- Роли: USER, BARISTA, MANAGER, ADMIN
- Выбор кофейни и создание заказов с кастомизацией (топинги)
- Сохранение любимых комбинаций напитков
- Оплата через Kaspi и Stripe
- Печать заказов на термопринтере (ESC/POS)
- Админ-панель: управление заказами, меню, кофейнями, пользователями

## Стек

- Java 17 + Spring Boot 3.2
- PostgreSQL 16 + Flyway
- Spring Security + JWT
- Swagger/OpenAPI (springdoc)
- Docker + Docker Compose

## Быстрый старт

### Требования

- Docker и Docker Compose
- Java 17+ и Maven (для локальной разработки)

### 1. Клонировать репозиторий

```bash
git clone <repo-url>
cd coffee-shop-system
```

### 2. Настроить переменные окружения

```bash
cp .env.example .env
# Отредактировать .env под ваши настройки
```

### 3. Запустить через Docker Compose

Только база данных (для локальной разработки):
```bash
docker compose up postgres -d
```

Полный стек (приложение + база данных):
```bash
cp .env.production.example .env
# Заполнить все переменные в .env
docker compose up -d
```

### 4. Локальная разработка

```bash
# Запустить PostgreSQL
docker compose up postgres -d

# Запустить приложение
./mvnw spring-boot:run
```

Приложение доступно на: http://localhost:8080

Swagger UI: http://localhost:8080/swagger-ui.html

## Тесты

```bash
./mvnw clean test
```

## API документация

После запуска приложения Swagger UI доступен по адресу:
http://localhost:8080/swagger-ui.html

OpenAPI JSON: http://localhost:8080/v3/api-docs

## Основные эндпоинты

### Аутентификация
- `POST /api/auth/request-code` — запрос OTP кода на email
- `POST /api/auth/verify-code` — верификация OTP, получение JWT
- `POST /api/auth/refresh` — обновление access токена

### Кофейни и меню
- `GET /api/shops` — список кофеен (сгруппировано по городу)
- `GET /api/shops/{id}` — детали кофейни
- `GET /api/shops/search?query=` — поиск кофеен
- `GET /api/products` — список товаров
- `GET /api/products/{id}` — товар с топингами
- `GET /api/toppings` — топинги (сгруппировано по типу)

### Заказы
- `POST /api/orders` — создать заказ
- `GET /api/orders` — список заказов пользователя
- `GET /api/orders/{id}` — детали заказа
- `POST /api/orders/{id}/cancel` — отменить заказ
- `POST /api/orders/{id}/pay` — оплатить заказ

### Избранное
- `POST /api/saved-combinations` — сохранить комбинацию
- `GET /api/saved-combinations` — список комбинаций
- `DELETE /api/saved-combinations/{id}` — удалить комбинацию
- `POST /api/favorites` — добавить в избранное
- `GET /api/favorites` — список избранного

### Оплата (вебхуки)
- `POST /api/payments/webhook/kaspi` — вебхук от Kaspi
- `POST /api/payments/webhook/stripe` — вебхук от Stripe

### Админ-панель
- `GET /api/admin/orders` — все заказы с фильтрами
- `PATCH /api/admin/orders/{id}/status` — изменить статус заказа
- `POST /api/admin/orders/{id}/print` — распечатать заказ
- `POST /api/admin/shops` — создать кофейню
- `PUT /api/admin/shops/{id}` — обновить кофейню
- `DELETE /api/admin/shops/{id}` — удалить кофейню
- `POST /api/admin/products` — создать товар
- `PUT /api/admin/products/{id}` — обновить товар
- `DELETE /api/admin/products/{id}` — удалить товар
- `POST /api/admin/toppings` — создать топинг
- `PUT /api/admin/toppings/{id}` — обновить топинг
- `DELETE /api/admin/toppings/{id}` — удалить топинг
- `GET /api/admin/users` — список пользователей

## Переменные окружения

| Переменная | Описание | Пример |
|---|---|---|
| `DB_HOST` | Хост PostgreSQL | `localhost` |
| `DB_PORT` | Порт PostgreSQL | `5432` |
| `DB_NAME` | Имя базы данных | `coffeeshop_dev` |
| `DB_USERNAME` | Пользователь БД | `coffeeshop` |
| `DB_PASSWORD` | Пароль БД | `secret` |
| `JWT_SECRET` | Секрет для JWT (мин. 256 бит) | `...` |
| `JWT_ACCESS_EXPIRATION` | Время жизни access токена (мс) | `900000` |
| `JWT_REFRESH_EXPIRATION` | Время жизни refresh токена (мс) | `604800000` |
| `MAIL_HOST` | SMTP хост | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP порт | `587` |
| `MAIL_USERNAME` | SMTP пользователь | `noreply@example.com` |
| `MAIL_PASSWORD` | SMTP пароль | `...` |
| `SERVER_PORT` | Порт сервера | `8080` |
| `SPRING_PROFILES_ACTIVE` | Профиль (`dev` или `prod`) | `dev` |
| `PRINTER_HOST` | IP/хост термопринтера | `localhost` |
| `PRINTER_PORT` | TCP-порт принтера | `9100` |
| `PRINTER_ENABLED` | Включить принтер (`true`/`false`) | `false` |
| `PRINTER_TIMEOUT_MS` | Таймаут подключения к принтеру (мс) | `5000` |
| `AUTO_PRINT_ENABLED` | Автопечать при новом заказе | `false` |
| `KASPI_API_KEY` | API-ключ Kaspi Pay | `...` |
| `KASPI_MERCHANT_ID` | Merchant ID Kaspi Pay | `...` |
| `STRIPE_SECRET_KEY` | Секретный ключ Stripe | `sk_test_...` |
| `STRIPE_WEBHOOK_SECRET` | Секрет вебхука Stripe | `whsec_...` |

Полный список — в файле `.env.production.example`.

## Роли пользователей

| Роль | Описание |
|---|---|
| `USER` | Обычный пользователь: заказы, избранное, оплата |
| `BARISTA` | Просмотр и обновление статусов заказов |
| `MANAGER` | Управление меню, кофейнями, просмотр пользователей |
| `ADMIN` | Полный доступ ко всем функциям |
