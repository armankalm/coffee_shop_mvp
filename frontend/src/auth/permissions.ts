export type Permission = string

export const ROLE_PERMISSIONS = {
  USER: ['orders:read', 'orders:create', 'orders:cancel'],
  BARISTA: ['orders:read', 'orders:update-status', 'orders:print'],
  MANAGER: ['orders:*', 'products:*', 'shops:*'],
  ADMIN: ['orders:*', 'products:*', 'shops:*', 'users:*', 'roles:*'],
} as const satisfies Record<string, readonly Permission[]>

export type KnownRole = keyof typeof ROLE_PERMISSIONS

export const KITCHEN_BOARD_PERMISSION = 'orders:update-status'

export function getRolePermissions(role: string | null | undefined): readonly Permission[] {
  if (!role) return []

  const normalizedRole = role.trim().toUpperCase() as KnownRole
  return ROLE_PERMISSIONS[normalizedRole] ?? []
}

function permissionMatches(grantedPermission: Permission, requestedPermission: Permission) {
  if (grantedPermission === requestedPermission || grantedPermission === '*') {
    return true
  }

  if (!grantedPermission.endsWith(':*')) {
    return false
  }

  return requestedPermission.startsWith(grantedPermission.slice(0, -1))
}

/**
 * Where a user should land right after login when no explicit destination was
 * requested: staff (those who can advance orders) go to their workspace,
 * regular customers to the shop locations screen.
 */
export function defaultLandingPath(role: string | null | undefined) {
  return hasPermission(role, KITCHEN_BOARD_PERMISSION) ? '/staff' : '/locations'
}

export function hasPermission(
  role: string | null | undefined,
  permission: Permission,
  permissions?: readonly Permission[] | null,
) {
  const grantedPermissions = permissions ?? getRolePermissions(role)

  return grantedPermissions.some((grantedPermission) => permissionMatches(grantedPermission, permission))
}
