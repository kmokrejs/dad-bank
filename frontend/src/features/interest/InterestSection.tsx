import { useState, type FormEvent } from 'react'
import { Alert, Button, Card, Field, Spinner, Stat, Table, type Column } from '../../components/ui'
import type { InterestRateChange } from '../../api/types'
import { t } from '../../i18n'
import { formatDate, formatRate, parseRate } from '../../money'
import { useInterestHistory, useSetInterestRate } from './queries'
import './InterestSection.css'

const columns: Column<InterestRateChange>[] = [
  { key: 'when', header: t('interest.when'), render: (r) => formatDate(r.createdAt) },
  { key: 'rate', header: t('interest.rateCol'), align: 'right', render: (r) => <strong>{formatRate(r.rateBps)}</strong> },
  { key: 'by', header: t('interest.setBy'), render: (r) => r.setBy },
  { key: 'note', header: t('interest.note'), render: (r) => r.note ?? <span className="muted">—</span> },
]

/** Admin: current bank-wide savings rate, form to change it, and the full change log. */
export function InterestSection() {
  const history = useInterestHistory()
  const setRate = useSetInterestRate()
  const [rate, setRateInput] = useState('')
  const [note, setNote] = useState('')
  const [rateError, setRateError] = useState<string | undefined>()

  const current = history.data?.[0]

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const bps = parseRate(rate)
    if (bps === null) { setRateError(t('interest.rateFormat')); return }
    setRateError(undefined)
    setRate.mutate({ rateBps: bps, note }, { onSuccess: () => { setRateInput(''); setNote('') } })
  }

  return (
    <Card title={t('interest.title')} icon="📈" collapsible defaultOpen>
      <div className="interest">
        <div className="interest__current">
          <Stat label={t('interest.current')} value={history.isPending ? '…' : formatRate(current?.rateBps ?? 0)} big />
          {current && <p className="text-sm muted">{t('interest.since', { date: formatDate(current.createdAt), by: current.setBy })}</p>}
        </div>
        <form className="interest__form stack" onSubmit={onSubmit} noValidate>
          <Field label={t('interest.newRate')} value={rate} onChange={(e) => setRateInput(e.target.value)} inputMode="decimal" placeholder={t('interest.ratePlaceholder')} hint={t('interest.rateHint')} error={rateError ?? setRate.error?.errors.rateBps} required />
          <Field label={t('interest.note')} value={note} onChange={(e) => setNote(e.target.value)} maxLength={140} placeholder={t('interest.notePlaceholder')} />
          {setRate.isError && !setRate.error.errors.rateBps && <Alert>{setRate.error.message}</Alert>}
          <div><Button type="submit" loading={setRate.isPending}>{t('interest.set')}</Button></div>
        </form>
      </div>
      <div className="interest__history">
        <h3 className="interest__history-title">{t('interest.history')}</h3>
        {history.isPending && <Spinner />}
        {history.isError && <Alert>{history.error.message}</Alert>}
        {history.data && <Table columns={columns} rows={history.data} rowKey={(r) => r.id} empty={t('interest.empty')} />}
      </div>
    </Card>
  )
}
