import { api } from './client'
import type { AccountType, PageResponse, TransactionResult, TransactionView } from './types'

export const transactionsApi = {
  list: (page = 0, size = 10, account: AccountType = 'CHECKING') =>
    api<PageResponse<TransactionView>>(`/api/transactions?account=${account}&page=${page}&size=${size}`),
  adminList: (accountId: number, page = 0, size = 10) =>
    api<PageResponse<TransactionView>>(`/api/admin/accounts/${accountId}/transactions?page=${page}&size=${size}`),
  transfer: (toAccountNumber: string, amountCents: number, note?: string) =>
    api<TransactionResult>('/api/transfers', {
      method: 'POST',
      body: JSON.stringify({ toAccountNumber, amountCents, note: note || undefined }),
    }),
  adminDeposit: (accountId: number, amountCents: number, note?: string) =>
    api<TransactionResult>(`/api/admin/accounts/${accountId}/deposit`, {
      method: 'POST',
      body: JSON.stringify({ amountCents, note: note || undefined }),
    }),
  adminWithdraw: (accountId: number, amountCents: number, note?: string) =>
    api<TransactionResult>(`/api/admin/accounts/${accountId}/withdraw`, {
      method: 'POST',
      body: JSON.stringify({ amountCents, note: note || undefined }),
    }),
}
