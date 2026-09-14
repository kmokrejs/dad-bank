export type Role = 'USER' | 'ADMIN'

export type AccountType = 'CHECKING' | 'SAVINGS'

export interface AccountDto {
  id: number
  type: AccountType
  accountNumber: string
  balanceCents: number
}

export interface UserView {
  id: number
  email: string
  username: string
  role: Role
  account: AccountDto | null
  savings: AccountDto | null
}

export interface AuthResponse {
  token: string
  user: UserView
}

export interface AdminAccountView {
  accountId: number
  userId: number
  username: string
  email: string
  role: Role
  accountType: AccountType
  accountNumber: string
  balanceCents: number
}

export interface ApiErrorBody {
  message: string
  errors: Record<string, string>
}

export type TransactionType = 'TRANSFER' | 'DEPOSIT' | 'WITHDRAWAL'

export interface TransactionView {
  id: number
  type: TransactionType
  direction: 'IN' | 'OUT'
  amountCents: number
  note: string | null
  counterparty: { accountNumber: string; username: string; accountType: AccountType } | null
  createdAt: string
}

export interface TransactionResult {
  transaction: TransactionView
  balanceCents: number
}

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface SavingsMoveResult {
  transaction: TransactionView
  checkingBalanceCents: number
  savingsBalanceCents: number
}

export interface InterestRateView {
  rateBps: number
}

export interface InterestRateChange {
  id: number
  rateBps: number
  setBy: string
  note: string | null
  createdAt: string
}

export type WithdrawalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface WithdrawalView {
  id: number
  accountId: number
  username: string
  accountNumber: string
  amountCents: number
  note: string | null
  status: WithdrawalStatus
  createdAt: string
  decidedBy: string | null
  decidedAt: string | null
  rejectionReason: string | null
}

export interface WithdrawalDecisionResult {
  withdrawal: WithdrawalView
  balanceCents: number
}
