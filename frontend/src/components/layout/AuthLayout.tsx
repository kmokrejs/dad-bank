import type { ReactNode } from 'react'
import { Card } from '../ui'
import { t } from '../../i18n'
import { Logo } from './Logo'
import './AuthLayout.css'

/** Centered single card for login / register. */
export function AuthLayout({ title, subtitle, children, footer }: { title: string; subtitle?: string; children: ReactNode; footer?: ReactNode }) {
  return (
    <div className="auth">
      <div className="auth__brand"><Logo size={32} /><span>{t('app.name')}</span></div>
      <Card className="auth__card">
        <h1 className="auth__title">{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
        <div className="mt-4">{children}</div>
      </Card>
      {footer && <p className="auth__footer text-sm muted">{footer}</p>}
    </div>
  )
}
