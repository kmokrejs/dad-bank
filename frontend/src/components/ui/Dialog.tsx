import { useEffect, useRef, type ReactNode } from 'react'
import { t } from '../../i18n'
import './Dialog.css'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: ReactNode
  children: ReactNode
}

/** Modal built on the native <dialog> element (focus trap, Esc and backdrop click close it). */
export function Dialog({ open, onClose, title, children }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (open && !el.open) el.showModal()
    if (!open && el.open) el.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      className="dialog"
      onClose={onClose}
      onClick={(e) => { if (e.target === ref.current) onClose() }}
    >
      <div className="dialog__panel">
        <header className="dialog__header">
          <h2 className="dialog__title">{title}</h2>
          <button type="button" className="dialog__close" aria-label={t('common.close')} onClick={onClose}>×</button>
        </header>
        <div className="dialog__body">{open && children}</div>
      </div>
    </dialog>
  )
}
