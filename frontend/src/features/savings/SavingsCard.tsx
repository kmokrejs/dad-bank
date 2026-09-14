import { useState } from 'react'
import { Alert, Button, Card, Stat } from '../../components/ui'
import type { AccountDto } from '../../api/types'
import { t } from '../../i18n'
import { formatMoney, formatRate } from '../../money'
import { MoveSavingsDialog, type MoveKind } from './MoveSavingsDialog'
import { useInterestRate, useOpenSavings } from './queries'
import './SavingsCard.css'

interface Props {
  savings: AccountDto | null
  checkingBalanceCents: number
}

/** Either an invitation to open a savings account, or the account itself with move-money actions. */
export function SavingsCard({ savings, checkingBalanceCents }: Props) {
  const rate = useInterestRate()
  const open = useOpenSavings()
  const [move, setMove] = useState<MoveKind | null>(null)

  const rateText = rate.data ? formatRate(rate.data.rateBps) : '…'

  if (!savings) {
    return (
      <Card className="savings savings--offer">
        <div className="savings__offer">
          <span className="savings__pig" aria-hidden>🐷</span>
          <div className="stack-sm">
            <h2 className="savings__title">{t('savings.offerTitle')}</h2>
            <p className="muted">{t('savings.offerText', { rate: rateText })}</p>
            {open.isError && <Alert>{open.error.message}</Alert>}
            <div>
              <Button onClick={() => open.mutate()} loading={open.isPending}>{t('savings.open')}</Button>
            </div>
          </div>
        </div>
      </Card>
    )
  }

  return (
    <>
      <Card
        className="savings"
        title={t('savings.title')}
        icon="🐷"
        actions={<span className="savings__rate">{t('savings.rate', { rate: rateText })}</span>}
      >
        <div className="savings__body">
          <Stat label={t('savings.balance')} value={formatMoney(savings.balanceCents)} big />
          <div className="savings__meta text-sm muted mono">{savings.accountNumber}</div>
          <div className="row savings__actions">
            <Button onClick={() => setMove('deposit')}>{t('savings.putIn')}</Button>
            <Button variant="secondary" onClick={() => setMove('withdraw')} disabled={savings.balanceCents === 0}>{t('savings.takeOut')}</Button>
          </div>
        </div>
      </Card>
      <MoveSavingsDialog
        kind={move}
        onClose={() => setMove(null)}
        checkingBalanceCents={checkingBalanceCents}
        savingsBalanceCents={savings.balanceCents}
      />
    </>
  )
}
