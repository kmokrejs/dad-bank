import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi } from '../../api/admin'
import { authApi } from '../../api/auth'
import { transactionsApi } from '../../api/transactions'
import type { ApiError } from '../../api/client'
import type { AdminAccountView, TransactionResult } from '../../api/types'

export const adminAccountsKey = ['admin', 'accounts'] as const

export function useAdminAccounts() {
  return useQuery<AdminAccountView[], ApiError>({
    queryKey: adminAccountsKey,
    queryFn: authApi.adminAccounts,
  })
}

type AdjustVars = { accountId: number; amountCents: number; note?: string }

function useAdjust(kind: 'deposit' | 'withdraw') {
  const qc = useQueryClient()
  return useMutation<TransactionResult, ApiError, AdjustVars>({
    mutationFn: ({ accountId, amountCents, note }) =>
      kind === 'deposit'
        ? transactionsApi.adminDeposit(accountId, amountCents, note)
        : transactionsApi.adminWithdraw(accountId, amountCents, note),
    onSuccess: (res, vars) => {
      qc.setQueryData<AdminAccountView[]>(adminAccountsKey, (rows) =>
        rows?.map((r) => (r.accountId === vars.accountId ? { ...r, balanceCents: res.balanceCents } : r)),
      )
      qc.invalidateQueries({ queryKey: adminAccountsKey })
      qc.invalidateQueries({ queryKey: ['me'] }) // admin may have adjusted their own account
      qc.invalidateQueries({ queryKey: ['transactions'] })
    },
  })
}

export const useAdminDeposit = () => useAdjust('deposit')
export const useAdminWithdraw = () => useAdjust('withdraw')

export function useResetPassword() {
  return useMutation<void, ApiError, { userId: number; newPassword: string }>({
    mutationFn: ({ userId, newPassword }) => adminApi.resetPassword(userId, newPassword),
  })
}
