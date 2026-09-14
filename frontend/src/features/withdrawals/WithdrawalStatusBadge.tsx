import type { WithdrawalStatus } from '../../api/types'
import { t } from '../../i18n'
import './WithdrawalStatusBadge.css'

const ICONS: Record<WithdrawalStatus, string> = { PENDING: '⏳', APPROVED: '✅', REJECTED: '❌' }
const KEYS = { PENDING: 'withdraw.pending', APPROVED: 'withdraw.approved', REJECTED: 'withdraw.rejected' } as const

export function WithdrawalStatusBadge({ status }: { status: WithdrawalStatus }) {
  return (
    <span className={`wstatus wstatus--${status.toLowerCase()}`}>
      <span aria-hidden>{ICONS[status]}</span> {t(KEYS[status])}
    </span>
  )
}
