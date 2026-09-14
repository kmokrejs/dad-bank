import type { ButtonHTMLAttributes } from 'react'
import './Button.css'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  loading?: boolean
  block?: boolean
}

export function Button({ variant = 'primary', size = 'md', loading, block, className = '', children, disabled, ...rest }: ButtonProps) {
  const cls = ['btn', `btn--${variant}`, `btn--${size}`, block ? 'btn--block' : '', className].filter(Boolean).join(' ')
  return (
    <button className={cls} disabled={disabled || loading} aria-busy={loading || undefined} {...rest}>
      {loading && <span className="btn__spinner" aria-hidden />}
      <span className="btn__label">{children}</span>
    </button>
  )
}
