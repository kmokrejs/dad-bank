import { useState } from 'react'
import { Alert, Button, Card, Spinner, Table, type Column } from '../../components/ui'
import type { WithdrawalView } from '../../api/types'
import { t } from '../../i18n'
import { formatDate, formatMoney } from '../../money'
import { RejectWithdrawalDialog } from './RejectWithdrawalDialog'
import { useApproveWithdrawal, usePendingWithdrawals } from './queries'

/** Admin queue: every kid's pending cash request, oldest first. */
export function PendingWithdrawalsCard() {
  const queue = usePendingWithdrawals()
  const approve = useApproveWithdrawal()
  const [rejecting, setRejecting] = useState<WithdrawalView | null>(null)
  const [failed, setFailed] = useState<string | null>(null)

  const count = queue.data?.totalItems ?? 0

  const columns: Column<WithdrawalView>[] = [
    { key: 'who', header: t('admin.user'), render: (w) => <><strong>{w.username}</strong><div className="text-sm muted">{formatDate(w.createdAt)}</div></> },
    { key: 'what', header: t('withdraw.what'), render: (w) => w.note ?? <span className="muted">—</span> },
    { key: 'amount', header: t('admin.balance'), align: 'right', render: (w) => <strong>{formatMoney(w.amountCents)}</strong> },
    {
      key: 'actions', header: '', align: 'right', render: (w) => (
        <div className="row" style={{ justifyContent: 'flex-end' }}>
          <Button size="sm" variant="secondary" onClick={() => setRejecting(w)}>{t('withdraw.reject')}</Button>
          <Button
            size="sm"
            loading={approve.isPending && approve.variables?.id === w.id}
            onClick={() => { setFailed(null); approve.mutate({ id: w.id }, { onError: (e) => setFailed(e.message) }) }}
          >
            {t('withdraw.approve')}
          </Button>
        </div>
      ),
    },
  ]

  return (
    <>
      <Card title={`${t('withdraw.queue')}${count ? ` (${count})` : ''}`} icon="💸" padded={false} collapsible defaultOpen>
        {queue.isPending && <div style={{ padding: 'var(--space-5)' }}><Spinner /></div>}
        {queue.isError && <div style={{ padding: 'var(--space-5)' }}><Alert>{queue.error.message}</Alert></div>}
        {failed && <div style={{ padding: 'var(--space-4) var(--space-5) 0' }}><Alert>{failed}</Alert></div>}
        {queue.data && <Table columns={columns} rows={queue.data.items} rowKey={(w) => w.id} empty={t('withdraw.queueEmpty')} />}
      </Card>
      <RejectWithdrawalDialog request={rejecting} onClose={() => setRejecting(null)} />
    </>
  )
}
