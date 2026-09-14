import { useState } from 'react'
import { Button, Spinner } from '../../components/ui'
import { t } from '../../i18n'
import { formatDate, formatMoney } from '../../money'
import { WithdrawalStatusBadge } from './WithdrawalStatusBadge'
import { useMyWithdrawals } from './queries'
import './MyWithdrawalsList.css'

/** The kid's own cash requests with their current status (and the reason when refused). */
export function MyWithdrawalsList() {
  const [page, setPage] = useState(0)
  const q = useMyWithdrawals(page)

  if (q.isPending) return <Spinner />
  if (q.isError) return <p className="muted">{q.error.message}</p>
  if (!q.data || q.data.totalItems === 0) return <p className="muted">{t('withdraw.empty')}</p>

  return (
    <div aria-busy={q.isFetching || undefined}>
      <ul className="wlist">
        {q.data.items.map((w) => (
          <li key={w.id} className="wlist__row">
            <div className="wlist__main">
              <div className="wlist__top">
                <WithdrawalStatusBadge status={w.status} />
                {w.note && <span className="wlist__note">{w.note}</span>}
              </div>
              <div className="text-sm muted">
                {formatDate(w.createdAt)}
                {w.status === 'APPROVED' && w.decidedBy && <> · {t('withdraw.approvedBy', { name: w.decidedBy })}</>}
              </div>
              {w.status === 'REJECTED' && w.rejectionReason && (
                <div className="wlist__reason text-sm">{t('withdraw.reason', { reason: w.rejectionReason })}</div>
              )}
            </div>
            <div className="wlist__amount mono">{formatMoney(w.amountCents)}</div>
          </li>
        ))}
      </ul>
      {q.data.totalPages > 1 && (
        <div className="wlist__pager">
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>{t('tx.newer')}</Button>
          <span className="text-sm muted">{t('tx.page', { page: q.data.page + 1, total: q.data.totalPages })}</span>
          <Button variant="secondary" size="sm" disabled={page >= q.data.totalPages - 1} onClick={() => setPage(page + 1)}>{t('tx.older')}</Button>
        </div>
      )}
    </div>
  )
}
