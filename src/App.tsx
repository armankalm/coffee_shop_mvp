import { Navigate, Route, Routes } from 'react-router-dom'

import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'
import { KITCHEN_BOARD_PERMISSION } from './auth/permissions'
import { AppLayout } from './layout/AppLayout'
import { KitchenLayout } from './layout/KitchenLayout'
import {
  CartScreen,
  CatalogScreen,
  EditProfileScreen,
  LocationsScreen,
  LoginScreen,
  OrderStatusScreen,
  ProductScreen,
  ProfileScreen,
  KitchenOrdersScreen,
  OrderBoardScreen,
  StaffHomeScreen,
  PosOrderScreen,
} from './screens'

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="login" element={<LoginScreen />} />
        <Route element={<RequireAuth />}>
          <Route index element={<LocationsScreen />} />
          <Route path="locations" element={<LocationsScreen />} />
          <Route path="profile" element={<ProfileScreen />} />
          <Route path="profile/edit" element={<EditProfileScreen />} />
          <Route path="catalog" element={<CatalogScreen />} />
          <Route path="product/:productId" element={<ProductScreen />} />
          <Route path="cart" element={<CartScreen />} />
          <Route path="order/:orderId" element={<OrderStatusScreen />} />
          <Route path="*" element={<LocationsScreen />} />
        </Route>
      </Route>
      <Route element={<RequireAuth />}>
        <Route element={<RequireRole permission={KITCHEN_BOARD_PERMISSION} />}>
          <Route path="staff" element={<StaffHomeScreen />} />
          <Route path="staff/pos" element={<PosOrderScreen />} />
          <Route path="orders" element={<KitchenLayout />}>
            <Route index element={<Navigate to="new" replace />} />
            <Route path="new" element={<KitchenOrdersScreen statusFilter="NEW" />} />
            <Route path="in-progress" element={<KitchenOrdersScreen statusFilter="IN_PROGRESS" />} />
            <Route path="ready" element={<KitchenOrdersScreen statusFilter="READY" />} />
          </Route>
          <Route path="board/:shopId" element={<OrderBoardScreen />} />
        </Route>
      </Route>
    </Routes>
  )
}

export default App
