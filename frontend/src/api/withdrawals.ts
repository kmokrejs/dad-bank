import { api } from './client'
import type { PageResponse, WithdrawalDecisionResult, WithdrawalView } from './types'

export const withdrawalsApi = {
  create: (amountCents: number, note?: string) =>
    api<WithdrawalView>('/api/withdrawals', { method: 'POST', body: JSON.stringify({ amountCents, note: note || undefined }) }),
  mine: (page = 0, size = 10) =>
    api<PageResponse<WithdrawalView>>(`/api/withdrawals?page=${page}&size=${size}`),
  pending: (page = 0, size = 20) =>
    api<PageResponse<WithdrawalView>>(`/api/admin/withdrawals/pending?page=${page}&size=${size}`),
  approve: (id: number) =>
    api<WithdrawalDecisionResult>(`/api/admin/withdrawals/${id}/approve`, { method: 'POST' }),
  reject: (id: number, reason: string) =>
    api<WithdrawalDecisionResult>(`/api/admin/withdrawals/${id}/reject`, { method: 'POST', body: JSON.stringify({ reason }) }),
}
