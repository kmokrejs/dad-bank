import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { interestAdminApi } from '../../api/savings'
import type { ApiError } from '../../api/client'
import type { InterestRateChange } from '../../api/types'
import { interestRateKey } from '../savings/queries'

export const interestHistoryKey = ['interest', 'history'] as const

export function useInterestHistory() {
  return useQuery<InterestRateChange[], ApiError>({ queryKey: interestHistoryKey, queryFn: interestAdminApi.history })
}

export function useSetInterestRate() {
  const qc = useQueryClient()
  return useMutation<InterestRateChange, ApiError, { rateBps: number; note?: string }>({
    mutationFn: ({ rateBps, note }) => interestAdminApi.set(rateBps, note),
    onSuccess: (change) => {
      qc.setQueryData<InterestRateChange[]>(interestHistoryKey, (rows) => [change, ...(rows ?? [])])
      qc.setQueryData(interestRateKey, { rateBps: change.rateBps })
      qc.invalidateQueries({ queryKey: ['interest'] })
    },
  })
}
