# drinkit copy front

React/Vite frontend for a Drinkit-style coffee ordering app. The app includes customer ordering screens and a protected kitchen board for barista/admin order-position handling.

## Features

- Email code login with stored access/refresh tokens.
- Location and coffee-shop selection.
- Product catalog by selected shop, product detail, toppings, favorites, and cart checkout.
- Profile, profile editing, order history, repeat-order flow, and order-status polling.
- Kitchen board routes for new, in-progress, and ready order positions with role/permission gating.

## Routes

- `/login` - email code authentication.
- `/locations` - city/shop selection.
- `/catalog` - selected-shop catalog.
- `/product/:productId` - product detail and add-to-cart.
- `/cart` - current selected-shop cart and checkout.
- `/order/:orderId` - order status and cancellation.
- `/profile` - user profile and order history.
- `/profile/edit` - profile editing.
- `/orders/new` - kitchen board for new positions.
- `/orders/in-progress` - kitchen board for positions in progress.
- `/orders/ready` - kitchen board for ready positions.

## Configuration

Create `.env` from `.env.example`:

```sh
VITE_API_BASE_URL=http://localhost:8080/api
```

Optional:

```sh
VITE_API_ORIGIN=http://localhost:8080
```

`VITE_API_ORIGIN` is used to resolve relative image paths. When omitted, it defaults to `http://localhost:8080`.

## Backend API

The frontend expects the backend endpoints used by `src/api/*`:

- Auth: request code, verify code, refresh token.
- Shops: cities, shops by city, shop search, shop details.
- Products: products by shop/category and product details.
- Favorites: list/add/remove favorite products.
- Orders: create order, user orders, order details, cancel order.
- Users: current user and profile update.

Authenticated requests send a bearer access token. On `401`, the API client attempts one refresh-token retry, stores the refreshed session, and clears the session if refresh fails.

## Authentication

Sessions are stored under `drinkit.auth` in `localStorage`. Backend-provided `permissions` are used when present; otherwise permissions are derived from the user role.

Kitchen board access requires `orders:update-status`. `BARISTA` has that exact permission, while `MANAGER` and `ADMIN` are granted through `orders:*`. `USER` cannot access `/orders/*`.

Development builds may display and auto-fill a backend `devCode` from the request-code response. Production builds ignore `devCode`.

## Development

```sh
npm install
npm run dev
npm run lint
npm test
npm run build
npm run preview
```

Stack: React 19, Vite, TypeScript, React Router, CSS Modules, framer-motion, Vitest, ESLint.
