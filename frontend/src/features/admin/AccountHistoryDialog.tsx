import { useState } from 'react'
import { Dialog } from '../../components/ui'
import type { AdminAccountView } from '../../api/types'
import { t } from '../../i18n'
import { formatMoney } from '../../money'
import { TransactionList } from '../transactions/TransactionList'
import { useAdminTransactions } from '../transactions/queries'

/** Read-only history of any account, for god mode. */
export function AccountHistoryDialog({ account, onClose }: { account: AdminAccountView | null; onClose: () => void }) {
  const [page, setPage] = useState(0)
  const tx = useAdminTransactions(account?.accountId ?? null, page)

  function close() { setPage(0); onClose() }

  return (
    <Dialog open={account !== null} onClose={close} title={account ? t('admin.historyTitle', { name: account.username }) : ''}>
      {account && (
        <div className="stack">
          <p className="text-sm muted">
            {t('admin.balanceLabel')} <strong>{formatMoney(account.balanceCents)}</strong> · <span className="mono">{account.accountNumber}</span>
          </p>
          <TransactionList data={tx.data} isPending={tx.isPending} isFetching={tx.isFetching} error={tx.error} page={page} onPageChange={setPage} ownerUsername={account.username} />
        </div>
      )}
    </Dialog>
  )
}
