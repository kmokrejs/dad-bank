import { useId, type InputHTMLAttributes, type ReactNode } from 'react'
import './Field.css'

interface FieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: ReactNode
  hint?: ReactNode
  error?: string
  /** Extra class for the <input> itself (e.g. "mono"). */
  inputClassName?: string
}

/** Label + input + hint/error, wired together for accessibility. */
export function Field({ label, hint, error, className = '', inputClassName = '', ...input }: FieldProps) {
  const id = useId()
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined
  return (
    <div className={`field ${error ? 'field--error' : ''} ${className}`}>
      <label className="field__label" htmlFor={id}>{label}</label>
      <input id={id} className={`field__input ${inputClassName}`} aria-invalid={!!error || undefined} aria-describedby={describedBy} {...input} />
      {error ? (
        <span id={`${id}-error`} className="field__error" role="alert">{error}</span>
      ) : hint ? (
        <span id={`${id}-hint`} className="field__hint">{hint}</span>
      ) : null}
    </div>
  )
}
