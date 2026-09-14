import { useState, type FormEvent } from 'react'
import { Alert, Button, Dialog, Field, MoneyField } from '../../components/ui'
import { t } from '../../i18n'
import { formatMoney, validateAmount } from '../../money'
import { useRequestWithdrawal } from './queries'

interface Props {
  open: boolean
  onClose: () => void
  balanceCents: number
}

/** Kid asks for cash. Nothing moves until the admin approves. */
export function RequestWithdrawalDialog({ open, onClose, balanceCents }: Props) {
  const request = useRequestWithdrawal()
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [amountError, setAmountError] = useState<string | undefined>()
  const [done, setDone] = useState(false)

  function close() {
    setAmount(''); setNote(''); setAmountError(undefined); setDone(false); request.reset()
    onClose()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const amt = validateAmount(amount, balanceCents)
    setAmountError(amt.ok ? undefined : amt.error)
    if (!amt.ok) return
    request.mutate({ amountCents: amt.cents, note }, { onSuccess: () => setDone(true) })
  }

  return (
    <Dialog open={open} onClose={close} title={`💸 ${t('withdraw.title')}`}>
      {done ? (
        <div className="stack">
          <Alert tone="success">{t('withdraw.sent')}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <Button type="button" onClick={close}>{t('admin.done')}</Button>
          </div>
        </div>
      ) : (
        <form className="stack" onSubmit={onSubmit} noValidate>
          <p className="text-sm muted">{t('withdraw.available', { amount: formatMoney(balanceCents) })}</p>
          <MoneyField label={t('transfer.amount')} value={amount} onChange={(e) => setAmount(e.target.value)} required autoFocus error={amountError ?? request.error?.errors.amountCents} />
          <Field label={t('withdraw.what')} value={note} onChange={(e) => setNote(e.target.value)} maxLength={140} placeholder={t('withdraw.whatPlaceholder')} error={request.error?.errors.note} />
          {request.isError && !request.error.errors.amountCents && !request.error.errors.note && <Alert>{request.error.message}</Alert>}
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <Button type="button" variant="secondary" onClick={close}>{t('admin.cancel')}</Button>
            <Button type="submit" loading={request.isPending}>{t('withdraw.ask')}</Button>
          </div>
        </form>
      )}
    </Dialog>
  )
}
