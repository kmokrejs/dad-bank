import { Field } from './Field'
import { t } from '../../i18n'
import type { ComponentProps } from 'react'

type FieldProps = ComponentProps<typeof Field>

/** Text input for an amount in CZK; parse with parseMoney() on submit. */
export function MoneyField(props: Omit<FieldProps, 'type' | 'inputMode'>) {
  return <Field inputMode="decimal" placeholder={t('common.amountPlaceholder')} autoComplete="off" {...props} />
}
