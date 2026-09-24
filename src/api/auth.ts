import { API_BASE_URL, apiPost } from './client'

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  email: string
  role: string
  permissions?: string[]
}

export function requestCode(email: string) {
  return apiPost<{ message: string; devCode?: string }>('/auth/request-code', { email })
}

/** Full-page redirect target: the backend sends the browser on to Google. */
export const GOOGLE_SIGN_IN_URL = `${API_BASE_URL}/auth/oauth/google`

export function verifyCode(email: string, code: string) {
  return apiPost<AuthResponse>('/auth/verify-code', { email, code })
}
