import type { ReactNode } from 'react'
import './Alert.css'

export function Alert({ tone = 'danger', children }: { tone?: 'danger' | 'success' | 'info'; children: ReactNode }) {
  return <div className={`alert alert--${tone}`} role={tone === 'danger' ? 'alert' : 'status'}>{children}</div>
}
