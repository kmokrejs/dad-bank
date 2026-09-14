import type { ReactNode } from 'react'
import './Stat.css'

interface StatProps {
  label: ReactNode
  value: ReactNode
  icon?: string
  mono?: boolean
  big?: boolean
}

/** Label over a large value — balances, counts, account numbers. Optional emoji icon on the left. */
export function Stat({ label, value, icon, mono, big }: StatProps) {
  return (
    <div className={`stat ${icon ? 'stat--with-icon' : ''}`}>
      {icon && <span className="stat__icon" aria-hidden>{icon}</span>}
      <div className="stat__body">
        <div className="stat__label">{label}</div>
        <div className={`stat__value ${mono ? 'mono' : ''} ${big ? 'stat__value--big' : ''}`}>{value}</div>
      </div>
    </div>
  )
}
