import { apiGet, apiPatch } from './client'
import type { CoffeeShopDto } from './shops'

export type UserDto = {
  id: number
  email: string
  name: string | null
  phone: string | null
  role: string
  createdAt: string
  coffeeShopId: number | null
  coffeeShopName: string | null
  coffeeShop: CoffeeShopDto | null
  assignedShops?: CoffeeShopDto[] | null
}

export type UpdateProfileRequest = {
  name?: string
  phone?: string
}

export function getCurrentUser() {
  return apiGet<UserDto>('/users/me')
}

export function updateProfile(request: UpdateProfileRequest) {
  return apiPatch<UserDto>('/users/me', request)
}
