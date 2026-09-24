import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useAuth } from './AuthContext'
import { hasPermission } from './permissions'

type RequireRoleProps = {
  permission: string
}

export function RequireRole({ permission }: RequireRoleProps) {
  const { session } = useAuth()
  const location = useLocation()

  if (!session || !hasPermission(session.role, permission, session.permissions)) {
    return <Navigate to="/" replace state={{ from: location }} />
  }

  return <Outlet />
}
