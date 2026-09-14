import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthLayout } from '../components/layout/AuthLayout'
import { Alert, Button, Field } from '../components/ui'
import { useRegister } from '../features/auth/queries'
import { t } from '../i18n'

export function RegisterPage() {
  const register = useRegister()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  const fieldErrors = register.error?.errors ?? {}
  const generalError = register.isError && Object.keys(fieldErrors).length === 0 ? register.error.message : null

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    register.mutate({ email, username, password }, { onSuccess: () => navigate('/', { replace: true }) })
  }

  return (
    <AuthLayout
      title={t('auth.registerTitle')}
      subtitle={t('auth.registerSubtitle')}
      footer={<>{t('auth.haveAccount')} <Link to="/login">{t('auth.logIn')}</Link></>}
    >
      <form className="stack" onSubmit={onSubmit} noValidate>
        <Field label={t('auth.email')} type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required autoFocus error={fieldErrors.email} />
        <Field label={t('auth.username')} value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required hint={t('auth.usernameHint')} error={fieldErrors.username} />
        <Field label={t('auth.password')} type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" required hint={t('auth.passwordHint')} error={fieldErrors.password} />
        {generalError && <Alert>{generalError}</Alert>}
        <Button type="submit" block loading={register.isPending}>{t('auth.register')}</Button>
      </form>
    </AuthLayout>
  )
}
