import { useState } from 'react'
import { AppShell } from '../components/layout/AppShell'
import { Badge, Button, Card, Stat } from '../components/ui'
import type { AccountType } from '../api/types'
import { useMe } from '../features/auth/queries'
import { SavingsCard } from '../features/savings/SavingsCard'
import { SendMoneyDialog } from '../features/transfers/SendMoneyDialog'
import { MyWithdrawalsList } from '../features/withdrawals/MyWithdrawalsList'
import { RequestWithdrawalDialog } from '../features/withdrawals/RequestWithdrawalDialog'
import { TransactionList } from '../features/transactions/TransactionList'
import { useTransactions } from '../features/transactions/queries'
import { t } from '../i18n'
import { formatMoney } from '../money'
import './DashboardPage.css'

export function DashboardPage() {
  const { data: user } = useMe()
  const [page, setPage] = useState(0)
  const [scope, setScope] = useState<AccountType>('CHECKING')
  const [sendOpen, setSendOpen] = useState(false)
  const [askOpen, setAskOpen] = useState(false)
  const tx = useTransactions(page, scope)
  if (!user) return null

  function switchScope(next: AccountType) { setScope(next); setPage(0) }

  return (
    <AppShell>
      <div className="stack">
        <div className="row-between dashboard__hello">
          <h1>{t('dashboard.hello', { name: user.username })}</h1>
          {user.role === 'ADMIN' && <Badge tone="primary">{t('dashboard.godMode')}</Badge>}
        </div>

        {user.account ? (
          <>
            <div className="dashboard__grid">
              <Card className="card--accent">
                <Stat icon="🪙" label={t('dashboard.balance')} value={formatMoney(user.account.balanceCents)} big />
                <div className="row dashboard__send">
                  <Button variant="secondary" onClick={() => setSendOpen(true)}>✈️ {t('dashboard.sendMoney')}</Button>
                  <Button variant="secondary" onClick={() => setAskOpen(true)}>💸 {t('withdraw.ask')}</Button>
                </div>
              </Card>
              <Card>
                <Stat icon="🏦" label={t('dashboard.accountNumber')} value={user.account.accountNumber} mono />
                <p className="text-sm muted" style={{ marginTop: 'var(--space-2)' }}>{t('dashboard.shareHint')}</p>
              </Card>
            </div>

            <SavingsCard savings={user.savings} checkingBalanceCents={user.account.balanceCents} />

            <RequestWithdrawalDialog open={askOpen} onClose={() => setAskOpen(false)} balanceCents={user.account.balanceCents} />
            <SendMoneyDialog open={sendOpen} onClose={() => setSendOpen(false)} balanceCents={user.account.balanceCents} ownAccountNumber={user.account.accountNumber} />
          </>
        ) : (
          <Card><p className="muted">{t('dashboard.noAccount')}</p></Card>
        )}

        <Card title={t('withdraw.mine')} icon="💸" collapsible defaultOpen={false}>
          <MyWithdrawalsList />
        </Card>

        <Card title={t('dashboard.transactions')} icon="📒" collapsible defaultOpen={false}>
          {user.savings && (
            <div className="row dashboard__scope" role="tablist">
              <Button size="sm" variant={scope === 'CHECKING' ? 'primary' : 'secondary'} role="tab" aria-selected={scope === 'CHECKING'} onClick={() => switchScope('CHECKING')}>{t('dashboard.scopeMain')}</Button>
              <Button size="sm" variant={scope === 'SAVINGS' ? 'primary' : 'secondary'} role="tab" aria-selected={scope === 'SAVINGS'} onClick={() => switchScope('SAVINGS')}>{t('dashboard.scopeSavings')}</Button>
            </div>
          )}
          <TransactionList data={tx.data} isPending={tx.isPending} isFetching={tx.isFetching} error={tx.error} page={page} onPageChange={setPage} ownerUsername={user.username} />
        </Card>
      </div>
    </AppShell>
  )
}
