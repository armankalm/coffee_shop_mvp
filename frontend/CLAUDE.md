# Project Notes

## Architecture

- App entry is `src/main.tsx`.
- Provider order is `AuthProvider > ShopProvider > FavoritesProvider > CartProvider > App`.
- Customer routes render inside `AppLayout` with the mobile bottom navigation.
- Kitchen routes render outside `AppLayout` inside `RequireAuth > RequireRole > KitchenLayout`.
- Kitchen route access requires `orders:update-status`.

## API Pattern

- API helpers live under `src/api`.
- Use `apiGet`, `apiPost`, `apiPatch`, and `apiDelete` from `src/api/client.ts`.
- `apiGet`, `apiPatch`, and `apiDelete` are authenticated by default; `apiPost` takes an explicit auth flag.
- Relative asset paths should be passed through `resolveAssetUrl`.
- The client retries one `401` with `/auth/refresh`, then clears `drinkit.auth` on refresh failure.

## State And Storage

- Auth session key: `drinkit.auth`.
- Selected shop key: `drinkit.shop`.
- Cart key: `drinkit.cart`.
- Cart lines include `shopId`; checkout should only submit lines for the selected shop.
- Favorites are loaded from the backend when a session exists and are hidden when logged out.

## Kitchen Board

- Mock position API: `src/api/positions.ts`.
- Kitchen context: `src/kitchen/KitchenBoardContext.tsx`.
- Status chain: `NEW -> IN_PROGRESS -> READY -> COMPLETED`.
- `KitchenBoardProvider` loads positions, exposes counts, performs optimistic status updates, and rolls back on API failure.
- Screens under `/orders/*` must render loading/error states before showing an empty queue.
- Kitchen position cards use framer-motion layout animations through `OrderPositionsGrid`.

## Styling

- Global tokens are in `src/styles/tokens.css`.
- Component styles use CSS Modules.
- Global mobile-safe-area helpers are in `src/styles/global.css`.
- Customer screens are constrained to the mobile shell; kitchen screens use a wider tablet-style shell.

## Commands

```sh
npm run dev
npm run lint
npm test
npm run build
```

## Debugging

- To test role redirects, seed `drinkit.auth` with a role such as `USER`, `BARISTA`, `MANAGER`, or `ADMIN`.
- To test kitchen access without changing role mapping, seed `permissions: ["orders:update-status"]` in `drinkit.auth`.
- To test selected-shop flows, seed `drinkit.shop` with a backend `CoffeeShopDto` shape.
- To test cart checkout manually, seed `drinkit.cart` with `CartLine` objects that include the selected `shopId`.
- `/order/:orderId` polls order details every 30 seconds.
