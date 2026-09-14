import { Field } from './Field'
import type { ComponentProps } from 'react'

type FieldProps = ComponentProps<typeof Field>

/** Text input for an amount in CZK; parse with parseMoney() on submit. */
export function MoneyField(props: Omit<FieldProps, 'type' | 'inputMode'>) {
  return <Field inputMode="decimal" placeholder="0,00" autoComplete="off" {...props} />
}
