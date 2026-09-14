import { useMutation, useQueryClient } from '@tanstack/react-query'
import { transactionsApi } from '../../api/transactions'
import type { ApiError } from '../../api/client'
import type { TransactionResult, UserView } from '../../api/types'
import { meQueryKey } from '../auth/queries'
import { adminAccountsKey } from '../admin/queries'

export function useTransfer() {
  const qc = useQueryClient()
  return useMutation<TransactionResult, ApiError, { toAccountNumber: string; amountCents: number; note?: string }>({
    mutationFn: ({ toAccountNumber, amountCents, note }) => transactionsApi.transfer(toAccountNumber, amountCents, note),
    onSuccess: (res) => {
      // Update the balance immediately from the response, then refetch to be safe.
      qc.setQueryData<UserView | null>(meQueryKey, (me) =>
        me?.account ? { ...me, account: { ...me.account, balanceCents: res.balanceCents } } : me,
      )
      qc.invalidateQueries({ queryKey: meQueryKey })
      qc.invalidateQueries({ queryKey: ['transactions'] })
      qc.invalidateQueries({ queryKey: adminAccountsKey }) // admin sending money must see fresh balances on All accounts
    },
  })
}
