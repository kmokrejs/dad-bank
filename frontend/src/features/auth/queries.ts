import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '../../api/auth'
import { ApiError, tokenStore } from '../../api/client'
import type { AuthResponse, UserView } from '../../api/types'

export const meQueryKey = ['me'] as const

/** Current user; `null` when logged out. Resolves without a request when no token is stored. */
export function useMe() {
  return useQuery<UserView | null, ApiError>({
    queryKey: meQueryKey,
    queryFn: async () => {
      if (!tokenStore.get()) return null
      try {
        return await authApi.me()
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) {
          tokenStore.clear()
          return null
        }
        throw e
      }
    },
  })
}

function useSetSession() {
  const qc = useQueryClient()
  return (res: AuthResponse) => {
    tokenStore.set(res.token)
    qc.setQueryData<UserView | null>(meQueryKey, res.user)
  }
}

export function useLogin() {
  const setSession = useSetSession()
  return useMutation<AuthResponse, ApiError, { login: string; password: string }>({
    mutationFn: ({ login, password }) => authApi.login(login, password),
    onSuccess: setSession,
  })
}

export function useRegister() {
  const setSession = useSetSession()
  return useMutation<AuthResponse, ApiError, { email: string; username: string; password: string }>({
    mutationFn: ({ email, username, password }) => authApi.register(email, username, password),
    onSuccess: setSession,
  })
}

export function useLogout() {
  const qc = useQueryClient()
  return () => {
    tokenStore.clear()
    qc.setQueryData<UserView | null>(meQueryKey, null)
    qc.removeQueries({ predicate: (q) => q.queryKey[0] !== 'me' })
  }
}
