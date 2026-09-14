import { useState, type FormEvent } from 'react'
import { Alert, Button, Field, MoneyField } from '../../components/ui'
import { t } from '../../i18n'
import { formatMoney, validateAccountNumber, validateAmount } from '../../money'
import { useTransfer } from './queries'

export function SendMoneyForm({ balanceCents, ownAccountNumber }: { balanceCents: number; ownAccountNumber: string }) {
  const transfer = useTransfer()
  const [to, setTo] = useState('')
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [amountError, setAmountError] = useState<string | undefined>()
  const [toError, setToError] = useState<string | undefined>()
  const [success, setSuccess] = useState<string | null>(null)

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSuccess(null)
    const toErr = validateAccountNumber(to, ownAccountNumber)
    const amt = validateAmount(amount, balanceCents)
    setToError(toErr ?? undefined)
    setAmountError(amt.ok ? undefined : amt.error)
    if (toErr || !amt.ok) return
    const cents = amt.cents
    transfer.mutate(
      { toAccountNumber: to, amountCents: cents, note },
      {
        onSuccess: (res) => {
          setSuccess(t('transfer.sent', { amount: formatMoney(cents), name: res.transaction.counterparty?.username ?? to }))
          setTo(''); setAmount(''); setNote('')
        },
      },
    )
  }

  const serverFieldErrors = transfer.error?.errors ?? {}

  return (
    <form className="stack" onSubmit={onSubmit} noValidate>
      <Field label={t('transfer.recipient')} value={to} onChange={(e) => setTo(e.target.value)} placeholder="DB-1234-5678-9012" inputClassName="mono" required error={toError ?? serverFieldErrors.toAccountNumber} />
      <MoneyField label={t('transfer.amount')} value={amount} onChange={(e) => setAmount(e.target.value)} required error={amountError ?? serverFieldErrors.amountCents} />
      <Field label={t('transfer.note')} value={note} onChange={(e) => setNote(e.target.value)} maxLength={140} placeholder={t('transfer.notePlaceholder')} />
      {transfer.isError && Object.keys(serverFieldErrors).length === 0 && <Alert>{transfer.error.message}</Alert>}
      {success && <Alert tone="success">{success}</Alert>}
      <div>
        <Button type="submit" loading={transfer.isPending}>{t('transfer.submit')}</Button>
      </div>
    </form>
  )
}
