import { useState } from 'react'
import { AppShell } from '../components/layout/AppShell'
import { Alert, Badge, Button, Card, Spinner, Table, type Column } from '../components/ui'
import { AccountHistoryDialog } from '../features/admin/AccountHistoryDialog'
import { AdjustMoneyDialog, type AdjustMode } from '../features/admin/AdjustMoneyDialog'
import { ResetPasswordDialog } from '../features/admin/ResetPasswordDialog'
import { useAdminAccounts } from '../features/admin/queries'
import { InterestSection } from '../features/interest/InterestSection'
import { PendingWithdrawalsCard } from '../features/withdrawals/PendingWithdrawalsCard'
import type { AccountType, AdminAccountView } from '../api/types'
import { t } from '../i18n'
import { formatMoney } from '../money'

type Action = { account: AdminAccountView; kind: AdjustMode | 'history' | 'password' } | null

export function AdminPage() {
  const accounts = useAdminAccounts()
  const [action, setAction] = useState<Action>(null)
  const [tab, setTab] = useState<AccountType>('CHECKING')

  // One tab per account type; rows sorted by username so a family reads top-to-bottom.
  const rows = (accounts.data ?? [])
    .filter((a) => a.accountType === tab)
    .sort((a, b) => a.username.localeCompare(b.username))
  const savingsCount = accounts.data?.filter((a) => a.accountType === 'SAVINGS').length ?? 0
  const checkingCount = (accounts.data?.length ?? 0) - savingsCount

  // Always render the row's *current* data in the dialog so balances stay fresh after a mutation.
  const current = action ? accounts.data?.find((a) => a.accountId === action.account.accountId) ?? action.account : null

  const columns: Column<AdminAccountView>[] = [
    { key: 'user', header: t('admin.user'), render: (a) => <><strong>{a.username}</strong><div className="text-sm muted">{a.email}</div></> },
    { key: 'role', header: t('admin.role'), render: (a) => <Badge tone={a.role === 'ADMIN' ? 'primary' : 'neutral'}>{a.role}</Badge> },
    { key: 'account', header: t('admin.accountNumber'), render: (a) => <span className="mono">{a.accountNumber}</span> },
    { key: 'balance', header: t('admin.balance'), align: 'right', render: (a) => <strong>{formatMoney(a.balanceCents)}</strong> },
    {
      key: 'actions', header: '', align: 'right', render: (a) => (
        <div className="row" style={{ justifyContent: 'flex-end' }}>
          {a.accountType === 'CHECKING' && <Button size="sm" variant="ghost" onClick={() => setAction({ account: a, kind: 'password' })}>{t('admin.resetPassword')}</Button>}
          <Button size="sm" variant="ghost" onClick={() => setAction({ account: a, kind: 'history' })}>{t('admin.history')}</Button>
          <Button size="sm" variant="secondary" onClick={() => setAction({ account: a, kind: 'withdraw' })}>{t('admin.takeMoney')}</Button>
          <Button size="sm" variant="secondary" onClick={() => setAction({ account: a, kind: 'deposit' })}>{t('admin.addMoney')}</Button>
        </div>
      ),
    },
  ]


  return (
    <AppShell>
      <div className="stack">
        <h1>{t('admin.title')}</h1>
        <PendingWithdrawalsCard />
        <InterestSection />
        {accounts.isPending && <Spinner />}
        {accounts.isError && <Alert>{accounts.error.message}</Alert>}
        {accounts.data && (
          <Card
            padded={false}
            title={rows.length === 1 ? t('admin.countOne') : t('admin.count', { count: rows.length })}
            actions={
              <div className="row" role="tablist">
                <Button size="sm" variant={tab === 'CHECKING' ? 'primary' : 'secondary'} role="tab" aria-selected={tab === 'CHECKING'} onClick={() => setTab('CHECKING')}>🏦 {t('admin.tabChecking')} ({checkingCount})</Button>
                <Button size="sm" variant={tab === 'SAVINGS' ? 'primary' : 'secondary'} role="tab" aria-selected={tab === 'SAVINGS'} onClick={() => setTab('SAVINGS')}>🐷 {t('admin.tabSavings')} ({savingsCount})</Button>
              </div>
            }
          >
            <Table columns={columns} rows={rows} rowKey={(a) => a.accountId} empty={t(tab === 'SAVINGS' ? 'admin.noSavings' : 'admin.noAccounts')} />
          </Card>
        )}
      </div>
      <AdjustMoneyDialog account={action && (action.kind === 'deposit' || action.kind === 'withdraw') ? current : null} mode={action?.kind === 'withdraw' ? 'withdraw' : 'deposit'} onClose={() => setAction(null)} />
      <AccountHistoryDialog account={action?.kind === 'history' ? current : null} onClose={() => setAction(null)} />
      <ResetPasswordDialog account={action?.kind === 'password' ? current : null} onClose={() => setAction(null)} />
    </AppShell>
  )
}
