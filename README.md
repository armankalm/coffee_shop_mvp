# Coffee MVP

Приложение для кофейни: заказ напитков с телефона, кухонная доска для бариста,
POS-терминал и табло готовых заказов.

| Папка | Что внутри |
|---|---|
| [`backend/`](backend/README.md) | REST API на Spring Boot 3 (Java 17), PostgreSQL + Flyway, JWT |
| [`frontend/`](frontend/README.md) | React 19 + Vite, мобильный интерфейс клиента и экраны персонала |
| `docker/prod.Dockerfile` | Продакшн-образ: фронт собирается и кладётся внутрь jar |
| `render.yaml` | Blueprint для Render: один веб-сервис |

## Локальная разработка

```bash
# База данных
cd backend
docker compose up postgres -d

# API: http://localhost:8080, Swagger: http://localhost:8080/swagger-ui.html
./mvnw spring-boot:run

# Фронт: http://localhost:5173
cd ../frontend
cp .env.example .env
npm install
npm run dev
```

Без `BREVO_API_KEY` бэк работает в dev-режиме: код входа не отправляется письмом,
а возвращается в ответе и подставляется на экране входа.

## Продакшн

Один контейнер: Spring Boot отдаёт и `/api`, и собранный фронт (с fallback на
`index.html` для клиентских маршрутов). Фронт обращается к API на своём же домене,
поэтому CORS и отдельный адрес фронта не нужны.

Внешние сервисы:

- **Supabase Postgres** — база. Подключение через *Session pooler*: прямой хост
  Supabase доступен только по IPv6, которого у Render нет.
- **Supabase Storage** — картинки товаров (публичный бакет `products`). Диск Render
  очищается при каждом деплое.
- **Brevo** — письма с кодом входа через HTTP API. SMTP на бесплатном Render
  заблокирован.
- **Google OAuth** — вход через Google. Redirect URI:
  `https://<service>.onrender.com/api/auth/oauth/google/callback`.

### Деплой на Render

1. Render Dashboard → **New → Blueprint** → выбрать этот репозиторий.
2. Заполнить переменные с `sync: false` (список и пояснения — в `render.yaml`).
3. При первом старте Flyway создаст схему и тестовые данные в Supabase.

Проверить продакшн-образ локально:

```bash
docker build -f docker/prod.Dockerfile -t coffee-shop .
```
