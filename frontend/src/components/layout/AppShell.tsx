import type { ReactNode } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useLogout, useMe } from '../../features/auth/queries'
import { t } from '../../i18n'
import { Button } from '../ui'
import { Logo } from './Logo'
import './AppShell.css'

/** Header + content container for all signed-in pages. */
export function AppShell({ children }: { children: ReactNode }) {
  const { data: user } = useMe()
  const logout = useLogout()
  const navigate = useNavigate()

  return (
    <div className="shell">
      <header className="shell__header">
        <div className="shell__inner row-between">
          <Link to="/" className="shell__brand"><Logo /> {t('app.name')}</Link>
          <nav className="shell__nav">
            <NavLink to="/" end>{t('nav.overview')}</NavLink>
            {user?.role === 'ADMIN' && <NavLink to="/admin">{t('nav.admin')}</NavLink>}
          </nav>
          <div className="row">
            {user && <span className="shell__user text-sm muted">{user.username}</span>}
            <Button variant="secondary" size="sm" onClick={() => { logout(); navigate('/login') }}>{t('nav.logout')}</Button>
          </div>
        </div>
      </header>
      <main className="shell__main shell__inner">{children}</main>
    </div>
  )
}
