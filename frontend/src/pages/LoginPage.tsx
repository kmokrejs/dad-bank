import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthLayout } from '../components/layout/AuthLayout'
import { Alert, Button, Field } from '../components/ui'
import { useLogin } from '../features/auth/queries'
import { t } from '../i18n'

export function LoginPage() {
  const login = useLogin()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'

  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    login.mutate({ login: loginId, password }, { onSuccess: () => navigate(from, { replace: true }) })
  }

  return (
    <AuthLayout
      title={t('auth.loginTitle')}
      subtitle={t('auth.loginSubtitle')}
      footer={<>{t('auth.noAccount')} <Link to="/register">{t('auth.openOne')}</Link></>}
    >
      <form className="stack" onSubmit={onSubmit} noValidate>
        <Field label={t('auth.loginId')} value={loginId} onChange={(e) => setLoginId(e.target.value)} autoComplete="username" required autoFocus />
        <Field label={t('auth.password')} type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        {login.isError && <Alert>{login.error.message}</Alert>}
        <Button type="submit" block loading={login.isPending}>{t('auth.login')}</Button>
      </form>
    </AuthLayout>
  )
}
