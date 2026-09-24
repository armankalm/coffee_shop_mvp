import { apiGet } from './client'

export type CityDto = {
  id: number
  name: string
  region: string
}

export type CoffeeShopDto = {
  id: number
  name: string
  city: CityDto
  address: string
  status: string
  statusNameRu: string
}

export function getCities() {
  return apiGet<CityDto[]>('/cities')
}

export function getShopsByCity(cityId: number) {
  return apiGet<CoffeeShopDto[]>(`/cities/${cityId}/shops`)
}

export function getShopsGroupedByCity() {
  return apiGet<Record<string, CoffeeShopDto[]>>('/shops')
}

export function getShopById(shopId: number) {
  return apiGet<CoffeeShopDto>(`/shops/${shopId}`)
}

export function searchShops(query: string) {
  return apiGet<CoffeeShopDto[]>(`/shops/search?query=${encodeURIComponent(query)}`)
}
