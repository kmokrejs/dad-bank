import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { savingsApi } from '../../api/savings'
import type { ApiError } from '../../api/client'
import type { AccountDto, InterestRateView, SavingsMoveResult, UserView } from '../../api/types'
import { meQueryKey } from '../auth/queries'

export const interestRateKey = ['interest', 'current'] as const

export function useInterestRate() {
  return useQuery<InterestRateView, ApiError>({ queryKey: interestRateKey, queryFn: savingsApi.currentRate })
}

export function useOpenSavings() {
  const qc = useQueryClient()
  return useMutation<AccountDto, ApiError, void>({
    mutationFn: () => savingsApi.open(),
    onSuccess: (savings) => {
      qc.setQueryData<UserView | null>(meQueryKey, (me) => (me ? { ...me, savings } : me))
      qc.invalidateQueries({ queryKey: meQueryKey })
      qc.invalidateQueries({ queryKey: ['admin', 'accounts'] })
    },
  })
}

function useSavingsMove(kind: 'deposit' | 'withdraw') {
  const qc = useQueryClient()
  return useMutation<SavingsMoveResult, ApiError, { amountCents: number; note?: string }>({
    mutationFn: ({ amountCents, note }) => (kind === 'deposit' ? savingsApi.deposit(amountCents, note) : savingsApi.withdraw(amountCents, note)),
    onSuccess: (res) => {
      qc.setQueryData<UserView | null>(meQueryKey, (me) =>
        me?.account && me.savings
          ? { ...me, account: { ...me.account, balanceCents: res.checkingBalanceCents }, savings: { ...me.savings, balanceCents: res.savingsBalanceCents } }
          : me,
      )
      qc.invalidateQueries({ queryKey: meQueryKey })
      qc.invalidateQueries({ queryKey: ['transactions'] })
      qc.invalidateQueries({ queryKey: ['admin', 'accounts'] })
    },
  })
}

export const useSavingsDeposit = () => useSavingsMove('deposit')
export const useSavingsWithdraw = () => useSavingsMove('withdraw')
