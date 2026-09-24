# CLAUDE.md — Coffee Shop System

## Build & Test Commands

```bash
# Run all tests
./mvnw test

# Run tests quietly (show only failures)
./mvnw test -q

# Run a specific test class
./mvnw test -Dtest=OrderServiceTest

# Build (skip tests)
./mvnw package -DskipTests

# Run the application (requires PostgreSQL running)
./mvnw spring-boot:run

# Start only the database via Docker
docker compose up postgres -d
```

## Architecture

### Package Layout

```
com.coffeeshop.app
├── config/          - Spring Security, global exception handler, OpenAPI config
├── controller/      - REST controllers (thin layer, delegates to services)
├── domain/          - JPA entities and enums
├── dto/             - Request/response DTOs (validation annotations here)
├── repository/      - Spring Data JPA repositories
├── security/        - JWT provider, auth filter, JwtProperties
└── service/         - Business logic
    └── print/       - ESC/POS thermal printer integration and event listener
    └── payment/     - Payment provider abstraction (Kaspi, Stripe)
```

### Key Design Decisions

- **Authentication**: Passwordless email + OTP. Users auto-register on first login.
- **Authorization**: JWT access + refresh tokens. Roles: USER, BARISTA, MANAGER, ADMIN.
- **Payment providers**: Strategy pattern via `PaymentProviderService` interface, injected as a `Map<PaymentProvider, PaymentProviderService>`.
- **Printing**: `PrintService` interface with single `EscPosPrintService` implementation (TCP socket to ESC/POS printer). Auto-print on new order via Spring `@TransactionalEventListener(AFTER_COMMIT)`.
- **Profiles**: `dev` (show SQL, relaxed logging), `prod` (strict, no SQL logging), and `demo` (auto-seeds a full dataset on startup via `DataInitializer` + `DemoDataService`; uses `coffeeshop_demo` DB by default). `DataInitializer` and `DemoController` are `@Profile("demo")` only — the demo reset endpoint is not available in dev/prod.
- **Monorepo / SPA hosting**: this is `backend/` of the coffee_mvp monorepo. `docker/prod.Dockerfile` (repo root) builds `frontend/` and copies `dist/` into `src/main/resources/static`; `WebConfig` serves it with an `index.html` fallback for client routes, and `SecurityConfig` only protects `/api/**`. In prod `FRONTEND_URL`/`CORS_ALLOWED_ORIGINS` default to the app's own URL.
- **Reference tables**: Enum-like values (order statuses, shop statuses, user roles, topping types, product categories) are stored in DB reference tables (`ref_order_statuses`, `ref_shop_statuses`, `ref_user_roles`, `ref_topping_types`, `ref_product_categories`) managed via `ReferenceService`. JPA entities use `@ManyToOne` to the corresponding `Ref*` entity. Use `code` strings (e.g. `"NEW"`, `"ACTIVE"`) when referencing these in service logic. The old Java enums have been removed.
- **N+1 prevention**: All repository methods that load aggregate roots use `@Query` with explicit `JOIN FETCH` for associated collections. Use named variants like `findByIdWithDetails`, `findAllWithProducts`, `findAllWithIncompatibilities`. Never rely on lazy loading for collections accessed in service methods — add a JOIN FETCH variant instead.

## Auth Flow

1. `POST /api/auth/request-code` — client sends email; server auto-registers unknown users, generates a 6-digit OTP, emails it, and stores it hashed with expiry.
2. `POST /api/auth/verify-code` — client sends email + OTP; server validates, marks OTP used, returns `{ accessToken, refreshToken, email, role }`.
3. `POST /api/auth/refresh` — client sends `refreshToken`; server validates signature/expiry and issues new token pair.
4. Google: `GET /api/auth/oauth/google` redirects to Google; `/api/auth/oauth/google/callback` checks the `oauth_state` cookie, auto-registers the verified email and redirects to `<FRONTEND_URL>/auth/callback#accessToken=...&refreshToken=...&email=...&role=...` (or `#error=<code>`).
5. All protected endpoints expect `Authorization: Bearer <accessToken>`.

## Environment Variables

See `.env.example` for the full list. Key groups:

| Group | Variables |
|---|---|
| Database | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` |
| JWT | `JWT_SECRET` (min 256-bit), `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION` |
| Mail (OTP) | `BREVO_API_KEY`, `MAIL_FROM_ADDRESS`, `MAIL_FROM_NAME` (Brevo HTTP API; empty key = dev-mode, code returned in response) |
| Google sign-in | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `BACKEND_URL`, `FRONTEND_URL` |
| Image storage | `SUPABASE_URL`, `SUPABASE_SERVICE_KEY`, `SUPABASE_STORAGE_BUCKET` (empty URL = local disk) |
| Printer | `PRINTER_HOST`, `PRINTER_PORT`, `PRINTER_ENABLED`, `PRINTER_TIMEOUT_MS`, `AUTO_PRINT_ENABLED` |
| Kaspi | `KASPI_API_KEY`, `KASPI_MERCHANT_ID` |

## Testing Notes

- Unit tests use Mockito (`@ExtendWith(MockitoExtension.class)`).
- Controller tests use `@WebMvcTest` with `@Import` of security config.
- Integration/repository tests are in `EntityRelationshipTest` and require a real PostgreSQL instance (Testcontainers or an existing DB).
- The `PrintService` interface exists to allow mocking in tests — do not remove it.
