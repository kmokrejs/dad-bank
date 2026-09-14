import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { transactionsApi } from '../../api/transactions'
import type { ApiError } from '../../api/client'
import type { AccountType, PageResponse, TransactionView } from '../../api/types'

export const PAGE_SIZE = 10

/** Own history for one of the user's accounts. Key prefix ['transactions'] is what mutations invalidate. */
export function useTransactions(page: number, account: AccountType = 'CHECKING') {
  return useQuery<PageResponse<TransactionView>, ApiError>({
    queryKey: ['transactions', 'me', account, page],
    queryFn: () => transactionsApi.list(page, PAGE_SIZE, account),
    placeholderData: keepPreviousData, // keep the old page on screen while the next loads
  })
}

export function useAdminTransactions(accountId: number | null, page: number) {
  return useQuery<PageResponse<TransactionView>, ApiError>({
    queryKey: ['transactions', 'admin', accountId, page],
    queryFn: () => transactionsApi.adminList(accountId as number, page, PAGE_SIZE),
    enabled: accountId !== null,
    placeholderData: keepPreviousData,
  })
}
