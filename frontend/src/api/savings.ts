import { api } from './client'
import type { AccountDto, InterestRateChange, InterestRateView, SavingsMoveResult } from './types'

export const savingsApi = {
  open: () => api<AccountDto>('/api/savings', { method: 'POST' }),
  deposit: (amountCents: number, note?: string) =>
    api<SavingsMoveResult>('/api/savings/deposit', { method: 'POST', body: JSON.stringify({ amountCents, note: note || undefined }) }),
  withdraw: (amountCents: number, note?: string) =>
    api<SavingsMoveResult>('/api/savings/withdraw', { method: 'POST', body: JSON.stringify({ amountCents, note: note || undefined }) }),
  currentRate: () => api<InterestRateView>('/api/interest'),
}

export const interestAdminApi = {
  history: () => api<InterestRateChange[]>('/api/admin/interest'),
  set: (rateBps: number, note?: string) =>
    api<InterestRateChange>('/api/admin/interest', { method: 'POST', body: JSON.stringify({ rateBps, note: note || undefined }) }),
}
