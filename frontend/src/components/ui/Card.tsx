import { useState, type HTMLAttributes, type ReactNode } from 'react'
import './Card.css'

interface CardProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  title?: ReactNode
  icon?: string
  actions?: ReactNode
  padded?: boolean
  /** When set, the header toggles the body; `defaultOpen` picks the initial state. */
  collapsible?: boolean
  defaultOpen?: boolean
}

export function Card({ title, icon, actions, padded = true, collapsible, defaultOpen = true, className = '', children, ...rest }: CardProps) {
  const [open, setOpen] = useState(defaultOpen)
  const bodyVisible = !collapsible || open

  const heading = (
    <h2 className="card__title">
      {icon && <span className="card__icon" aria-hidden>{icon}</span>}
      {title}
    </h2>
  )

  return (
    <section className={`card ${className}`} {...rest}>
      {(title || actions) && (
        <header className={`card__header ${collapsible ? 'card__header--toggle' : ''} ${!bodyVisible ? 'card__header--closed' : ''}`}>
          {collapsible ? (
            <button type="button" className="card__toggle" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
              {heading}
              <span className={`card__chevron ${open ? 'card__chevron--open' : ''}`} aria-hidden>▾</span>
            </button>
          ) : heading}
          {actions && <div className="card__actions">{actions}</div>}
        </header>
      )}
      {bodyVisible && <div className={padded ? 'card__body' : ''}>{children}</div>}
    </section>
  )
}
