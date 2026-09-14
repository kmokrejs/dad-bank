import { useState, type FormEvent } from 'react'
import { Alert, Button, Dialog, Field, MoneyField } from '../../components/ui'
import type { AdminAccountView } from '../../api/types'
import { t } from '../../i18n'
import { formatMoney, validateAmount } from '../../money'
import { useAdminDeposit, useAdminWithdraw } from './queries'

export type AdjustMode = 'deposit' | 'withdraw'

interface Props {
  account: AdminAccountView | null
  mode: AdjustMode
  onClose: () => void
}

/** Admin deposit / withdrawal for one account. Withdrawals are capped at the current balance. */
export function AdjustMoneyDialog({ account, mode, onClose }: Props) {
  const deposit = useAdminDeposit()
  const withdraw = useAdminWithdraw()
  const mutation = mode === 'deposit' ? deposit : withdraw

  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [amountError, setAmountError] = useState<string | undefined>()

  function close() {
    setAmount(''); setNote(''); setAmountError(undefined); mutation.reset()
    onClose()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!account) return
    if (account.accountId == null) { setAmountError(t('admin.accountIdMissing')); return }
    const amt = validateAmount(amount, mode === 'withdraw' ? account.balanceCents : undefined)
    setAmountError(amt.ok ? undefined : amt.error)
    if (!amt.ok) return
    mutation.mutate({ accountId: account.accountId, amountCents: amt.cents, note }, { onSuccess: close })
  }

  const title = account ? t(mode === 'deposit' ? 'admin.depositTitle' : 'admin.withdrawTitle', { name: account.username }) : ''

  return (
    <Dialog open={account !== null} onClose={close} title={title}>
      {account && (
        <form className="stack" onSubmit={onSubmit} noValidate>
          <p className="text-sm muted">
            {t('admin.currentBalance')} <strong>{formatMoney(account.balanceCents)}</strong> · <span className="mono">{account.accountNumber}</span>
          </p>
          <MoneyField label={t('admin.amount')} value={amount} onChange={(e) => setAmount(e.target.value)} required autoFocus error={amountError ?? mutation.error?.errors.amountCents} />
          <Field label={t('admin.note')} value={note} onChange={(e) => setNote(e.target.value)} maxLength={140} placeholder={t(mode === 'deposit' ? 'admin.depositNote' : 'admin.withdrawNote')} />
          {mutation.isError && !mutation.error.errors.amountCents && <Alert>{mutation.error.message}</Alert>}
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <Button type="button" variant="secondary" onClick={close}>{t('admin.cancel')}</Button>
            <Button type="submit" variant={mode === 'withdraw' ? 'danger' : 'primary'} loading={mutation.isPending}>{t(mode === 'deposit' ? 'admin.depositSubmit' : 'admin.withdrawSubmit')}</Button>
          </div>
        </form>
      )}
    </Dialog>
  )
}
