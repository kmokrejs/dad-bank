import { Button, Spinner } from '../../components/ui'
import type { PageResponse, TransactionView } from '../../api/types'
import { t } from '../../i18n'
import { formatDate, formatMoney } from '../../money'
import './TransactionList.css'

interface Props {
  data: PageResponse<TransactionView> | undefined
  isPending: boolean
  isFetching?: boolean
  error?: { message: string } | null
  page: number
  onPageChange: (page: number) => void
  /** Username of the account owner, so own-account moves read as "to savings" instead of "to <me>". */
  ownerUsername?: string
}

function describe(tx: TransactionView, me: string | undefined): { icon: string; text: string } {
  const name = tx.counterparty?.username ?? t('tx.someone')
  switch (tx.type) {
    case 'DEPOSIT': return { icon: '🎁', text: t('tx.deposit') }
    case 'WITHDRAWAL': return { icon: '🛒', text: t('tx.withdrawal') }
    case 'TRANSFER': {
      if (tx.counterparty && tx.counterparty.username === me) {
        // A move between the user's own accounts.
        const otherIsSavings = tx.counterparty.accountType === 'SAVINGS'
        return tx.direction === 'IN'
          ? { icon: '🐷', text: t(otherIsSavings ? 'tx.fromSavings' : 'tx.fromMain') }
          : { icon: '🐷', text: t(otherIsSavings ? 'tx.toSavings' : 'tx.toMain') }
      }
      return tx.direction === 'IN'
        ? { icon: '📥', text: t('tx.from', { name }) }
        : { icon: '📤', text: t('tx.to', { name }) }
    }
  }
}

/** Ledger rows from one account's point of view, with simple prev/next paging. */
export function TransactionList({ data, isPending, isFetching, error, page, onPageChange, ownerUsername }: Props) {
  if (isPending) return <Spinner />
  if (error) return <p className="muted">{error.message}</p>
  if (!data || data.totalItems === 0) return <p className="muted">{t('tx.empty')}</p>

  return (
    <div className="txlist" aria-busy={isFetching || undefined}>
      <ul className="txlist__rows">
        {data.items.map((tx) => {
          const d = describe(tx, ownerUsername)
          return (
            <li key={tx.id} className="txlist__row">
              <span className="txlist__icon" aria-hidden>{d.icon}</span>
              <div className="txlist__main">
                <div className="txlist__desc">{d.text}</div>
                <div className="txlist__meta text-sm muted">
                  {formatDate(tx.createdAt)}
                  {tx.counterparty && <> · <span className="mono">{tx.counterparty.accountNumber}</span></>}
                  {tx.note && <> · {tx.note}</>}
                </div>
              </div>
              <div className={`txlist__amount mono ${tx.direction === 'IN' ? 'txlist__amount--in' : 'txlist__amount--out'}`}>
                {tx.direction === 'IN' ? '+' : '−'}{formatMoney(tx.amountCents)}
              </div>
            </li>
          )
        })}
      </ul>
      {data.totalPages > 1 && (
        <div className="txlist__pager">
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}>{t('tx.newer')}</Button>
          <span className="text-sm muted">{t('tx.page', { page: data.page + 1, total: data.totalPages })}</span>
          <Button variant="secondary" size="sm" disabled={page >= data.totalPages - 1} onClick={() => onPageChange(page + 1)}>{t('tx.older')}</Button>
        </div>
      )}
    </div>
  )
}
