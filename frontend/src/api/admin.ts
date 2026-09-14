import { api } from './client'

export const adminApi = {
  resetPassword: (userId: number, newPassword: string) =>
    api<void>(`/api/admin/users/${userId}/password`, { method: 'POST', body: JSON.stringify({ newPassword }) }),
}
