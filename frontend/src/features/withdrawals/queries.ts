import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query'
import { withdrawalsApi } from '../../api/withdrawals'
import type { ApiError } from '../../api/client'
import type { PageResponse, WithdrawalDecisionResult, WithdrawalView } from '../../api/types'

export const myWithdrawalsKey = ['withdrawals', 'me'] as const
export const pendingWithdrawalsKey = ['withdrawals', 'pending'] as const

/** The caller's own cash requests, every status, newest first. */
export function useMyWithdrawals(page = 0) {
  return useQuery<PageResponse<WithdrawalView>, ApiError>({
    queryKey: [...myWithdrawalsKey, page],
    queryFn: () => withdrawalsApi.mine(page),
    placeholderData: keepPreviousData,
  })
}

/** Admin queue, oldest first. */
export function usePendingWithdrawals() {
  return useQuery<PageResponse<WithdrawalView>, ApiError>({
    queryKey: pendingWithdrawalsKey,
    queryFn: () => withdrawalsApi.pending(),
  })
}

export function useRequestWithdrawal() {
  const qc = useQueryClient()
  return useMutation<WithdrawalView, ApiError, { amountCents: number; note?: string }>({
    mutationFn: ({ amountCents, note }) => withdrawalsApi.create(amountCents, note),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: myWithdrawalsKey })
      qc.invalidateQueries({ queryKey: pendingWithdrawalsKey })
    },
  })
}

type Decision = { id: number; reason?: string }

function useDecision(kind: 'approve' | 'reject') {
  const qc = useQueryClient()
  return useMutation<WithdrawalDecisionResult, ApiError, Decision>({
    mutationFn: ({ id, reason }) => (kind === 'approve' ? withdrawalsApi.approve(id) : withdrawalsApi.reject(id, reason ?? '')),
    onSuccess: () => {
      // An approval moves money and writes a ledger row, so refresh balances and history too.
      qc.invalidateQueries({ queryKey: ['withdrawals'] })
      qc.invalidateQueries({ queryKey: ['admin', 'accounts'] })
      qc.invalidateQueries({ queryKey: ['transactions'] })
      qc.invalidateQueries({ queryKey: ['me'] })
    },
  })
}

export const useApproveWithdrawal = () => useDecision('approve')
export const useRejectWithdrawal = () => useDecision('reject')
