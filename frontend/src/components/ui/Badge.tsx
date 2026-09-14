import type { ReactNode } from 'react'
import './Badge.css'

export function Badge({ tone = 'neutral', children }: { tone?: 'neutral' | 'primary'; children: ReactNode }) {
  return <span className={`badge badge--${tone}`}>{children}</span>
}
