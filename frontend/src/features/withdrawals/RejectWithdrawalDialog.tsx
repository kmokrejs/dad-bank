import { useState, type FormEvent } from 'react'
import { Alert, Button, Dialog, Field } from '../../components/ui'
import type { WithdrawalView } from '../../api/types'
import { t } from '../../i18n'
import { formatMoney } from '../../money'
import { useRejectWithdrawal } from './queries'

/** Admin refuses a request; a reason is required because the kid sees it. */
export function RejectWithdrawalDialog({ request, onClose }: { request: WithdrawalView | null; onClose: () => void }) {
  const reject = useRejectWithdrawal()
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | undefined>()

  function close() { setReason(''); setError(undefined); reject.reset(); onClose() }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!request) return
    if (!reason.trim()) { setError(t('withdraw.reasonRequired')); return }
    setError(undefined)
    reject.mutate({ id: request.id, reason: reason.trim() }, { onSuccess: close })
  }

  return (
    <Dialog open={request !== null} onClose={close} title={request ? t('withdraw.rejectTitle', { name: request.username }) : ''}>
      {request && (
        <form className="stack" onSubmit={onSubmit} noValidate>
          <p className="text-sm muted">
            {formatMoney(request.amountCents)}{request.note ? ` · ${request.note}` : ''}
          </p>
          <Field label={t('withdraw.reasonLabel')} value={reason} onChange={(e) => setReason(e.target.value)} maxLength={140} required autoFocus placeholder={t('withdraw.reasonPlaceholder')} error={error ?? reject.error?.errors.reason} />
          {reject.isError && !reject.error.errors.reason && <Alert>{reject.error.message}</Alert>}
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <Button type="button" variant="secondary" onClick={close}>{t('admin.cancel')}</Button>
            <Button type="submit" variant="danger" loading={reject.isPending}>{t('withdraw.reject')}</Button>
          </div>
        </form>
      )}
    </Dialog>
  )
}
