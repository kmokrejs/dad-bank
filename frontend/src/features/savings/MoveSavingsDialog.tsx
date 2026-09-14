import { useState, type FormEvent } from 'react'
import { Alert, Button, Dialog, Field, MoneyField } from '../../components/ui'
import { t } from '../../i18n'
import { formatMoney, validateAmount } from '../../money'
import { useSavingsDeposit, useSavingsWithdraw } from './queries'

export type MoveKind = 'deposit' | 'withdraw'

interface Props {
  kind: MoveKind | null
  onClose: () => void
  checkingBalanceCents: number
  savingsBalanceCents: number
}

/** Move money between the main account and savings. `kind` null = closed. */
export function MoveSavingsDialog({ kind, onClose, checkingBalanceCents, savingsBalanceCents }: Props) {
  const deposit = useSavingsDeposit()
  const withdraw = useSavingsWithdraw()
  const mutation = kind === 'withdraw' ? withdraw : deposit
  const max = kind === 'withdraw' ? savingsBalanceCents : checkingBalanceCents

  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [amountError, setAmountError] = useState<string | undefined>()

  function close() {
    setAmount(''); setNote(''); setAmountError(undefined); mutation.reset()
    onClose()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const amt = validateAmount(amount, max)
    setAmountError(amt.ok ? undefined : amt.error)
    if (!amt.ok) return
    mutation.mutate({ amountCents: amt.cents, note }, { onSuccess: close })
  }

  const isDeposit = kind !== 'withdraw'
  return (
    <Dialog open={kind !== null} onClose={close} title={isDeposit ? `🐷 ${t('savings.putIn')}` : `🐷 ${t('savings.takeOut')}`}>
      {kind && (
        <form className="stack" onSubmit={onSubmit} noValidate>
          <p className="text-sm muted">{t(isDeposit ? 'savings.availableMain' : 'savings.availableSavings', { amount: formatMoney(max) })}</p>
          <MoneyField label={t('transfer.amount')} value={amount} onChange={(e) => setAmount(e.target.value)} required autoFocus error={amountError ?? mutation.error?.errors.amountCents} />
          <Field label={t('transfer.note')} value={note} onChange={(e) => setNote(e.target.value)} maxLength={140} placeholder={t('savings.notePlaceholder')} />
          {mutation.isError && !mutation.error.errors.amountCents && <Alert>{mutation.error.message}</Alert>}
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <Button type="button" variant="secondary" onClick={close}>{t('admin.cancel')}</Button>
            <Button type="submit" loading={mutation.isPending}>{isDeposit ? t('savings.putIn') : t('savings.takeOut')}</Button>
          </div>
        </form>
      )}
    </Dialog>
  )
}
