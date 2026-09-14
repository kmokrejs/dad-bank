import { t } from '../../i18n'
import './Spinner.css'

export function Spinner() {
  return <span className="spinner" role="status" aria-label={t('common.loading')} />
}

export function PageSpinner() {
  return <div className="spinner-page"><Spinner /></div>
}
