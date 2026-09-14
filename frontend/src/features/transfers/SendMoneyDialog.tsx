import { Dialog } from '../../components/ui'
import { t } from '../../i18n'
import { SendMoneyForm } from './SendMoneyForm'

interface Props {
  open: boolean
  onClose: () => void
  balanceCents: number
  ownAccountNumber: string
}

export function SendMoneyDialog({ open, onClose, balanceCents, ownAccountNumber }: Props) {
  return (
    <Dialog open={open} onClose={onClose} title={`✈️ ${t('dashboard.sendMoney')}`}>
      <SendMoneyForm balanceCents={balanceCents} ownAccountNumber={ownAccountNumber} />
    </Dialog>
  )
}
