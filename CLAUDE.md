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
- **Profiles**: `dev` (show SQL, relaxed logging) and `prod` (strict, no SQL logging).

## Auth Flow

1. `POST /api/auth/request-code` — client sends email; server auto-registers unknown users, generates a 6-digit OTP, emails it, and stores it hashed with expiry.
2. `POST /api/auth/verify-code` — client sends email + OTP; server validates, marks OTP used, returns `{ accessToken, refreshToken, email, role }`.
3. `POST /api/auth/refresh` — client sends `refreshToken`; server validates signature/expiry and issues new token pair.
4. All protected endpoints expect `Authorization: Bearer <accessToken>`.

## Environment Variables

See `.env.example` for the full list. Key groups:

| Group | Variables |
|---|---|
| Database | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` |
| JWT | `JWT_SECRET` (min 256-bit), `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION` |
| Mail (OTP) | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` |
| Printer | `PRINTER_HOST`, `PRINTER_PORT`, `PRINTER_ENABLED`, `PRINTER_TIMEOUT_MS`, `AUTO_PRINT_ENABLED` |
| Kaspi | `KASPI_API_KEY`, `KASPI_MERCHANT_ID` |
| Stripe | `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` |

## Testing Notes

- Unit tests use Mockito (`@ExtendWith(MockitoExtension.class)`).
- Controller tests use `@WebMvcTest` with `@Import` of security config.
- Integration/repository tests are in `EntityRelationshipTest` and require a real PostgreSQL instance (Testcontainers or an existing DB).
- The `PrintService` interface exists to allow mocking in tests — do not remove it.
