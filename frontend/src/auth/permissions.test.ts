import { describe, expect, it } from 'vitest'

import {
  KITCHEN_BOARD_PERMISSION,
  defaultLandingPath,
  getRolePermissions,
  hasPermission,
} from './permissions'

describe('permissions', () => {
  it('grants kitchen-board access to baristas by exact permission', () => {
    expect(hasPermission('BARISTA', KITCHEN_BOARD_PERMISSION)).toBe(true)
  })

  it('does not grant kitchen-board access to regular users', () => {
    expect(hasPermission('USER', KITCHEN_BOARD_PERMISSION)).toBe(false)
  })

  it('matches wildcard permissions for managers and admins', () => {
    expect(hasPermission('MANAGER', KITCHEN_BOARD_PERMISSION)).toBe(true)
    expect(hasPermission('ADMIN', KITCHEN_BOARD_PERMISSION)).toBe(true)
  })

  it('uses explicit permissions when the session provides them', () => {
    expect(hasPermission('USER', KITCHEN_BOARD_PERMISSION, ['orders:*'])).toBe(true)
    expect(hasPermission('ADMIN', KITCHEN_BOARD_PERMISSION, ['orders:read'])).toBe(false)
  })

  it('normalizes role names before reading mapped permissions', () => {
    expect(getRolePermissions(' barista ')).toContain(KITCHEN_BOARD_PERMISSION)
  })

  it('routes staff to the workspace and customers to locations after login', () => {
    expect(defaultLandingPath('BARISTA')).toBe('/staff')
    expect(defaultLandingPath('MANAGER')).toBe('/staff')
    expect(defaultLandingPath('ADMIN')).toBe('/staff')
    expect(defaultLandingPath('USER')).toBe('/locations')
    expect(defaultLandingPath(null)).toBe('/locations')
  })
})
