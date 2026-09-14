import { api } from './client'
import type { AdminAccountView, AuthResponse, UserView } from './types'

export const authApi = {
  register: (email: string, username: string, password: string) =>
    api<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, username, password }),
    }),
  login: (login: string, password: string) =>
    api<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ login, password }),
    }),
  me: () => api<UserView>('/api/me'),
  adminAccounts: () => api<AdminAccountView[]>('/api/admin/accounts'),
}
