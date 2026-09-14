import { useState, type FormEvent } from 'react'
import { Alert, Button, Dialog, Field } from '../../components/ui'
import type { AdminAccountView } from '../../api/types'
import { t } from '../../i18n'
import { useResetPassword } from './queries'

const MIN_LENGTH = 8

/** Admin sets a new password for a user who forgot theirs. */
export function ResetPasswordDialog({ account, onClose }: { account: AdminAccountView | null; onClose: () => void }) {
  const reset = useResetPassword()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [errors, setErrors] = useState<{ password?: string; confirm?: string }>({})
  const [done, setDone] = useState(false)

  function close() {
    setPassword(''); setConfirm(''); setErrors({}); setDone(false); reset.reset()
    onClose()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!account) return
    const next: typeof errors = {}
    if (password.length < MIN_LENGTH) next.password = t('validation.passwordShort', { min: MIN_LENGTH })
    if (confirm !== password) next.confirm = t('validation.passwordMismatch')
    setErrors(next)
    if (next.password || next.confirm) return
    reset.mutate({ userId: account.userId, newPassword: password }, { onSuccess: () => setDone(true) })
  }

  return (
    <Dialog open={account !== null} onClose={close} title={account ? t('admin.resetTitle', { name: account.username }) : ''}>
      {account && (done ? (
        <div className="stack">
          <Alert tone="success">{t('admin.resetDone', { name: account.username })}</Alert>
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <Button type="button" onClick={close}>{t('admin.done')}</Button>
          </div>
        </div>
      ) : (
        <form className="stack" onSubmit={onSubmit} noValidate>
          <p className="text-sm muted">{account.email}</p>
          <Field label={t('admin.newPassword')} type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" required autoFocus hint={t('validation.passwordShort', { min: MIN_LENGTH })} error={errors.password ?? reset.error?.errors.newPassword} />
          <Field label={t('admin.repeatPassword')} type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" required error={errors.confirm} />
          {reset.isError && !reset.error.errors.newPassword && <Alert>{reset.error.message}</Alert>}
          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <Button type="button" variant="secondary" onClick={close}>{t('admin.cancel')}</Button>
            <Button type="submit" loading={reset.isPending}>{t('admin.setPassword')}</Button>
          </div>
        </form>
      ))}
    </Dialog>
  )
}
