import { intlLocale, t } from './i18n'

/** Money is stored as integer cents everywhere; format only at the edge. */
export function formatMoney(cents: number, currency = 'CZK'): string {
  return new Intl.NumberFormat(intlLocale, { style: 'currency', currency }).format(cents / 100)
}

/**
 * Parse user input like "50", "12,50", "1 250.5" into cents.
 * Returns null for anything that isn't a positive amount with at most 2 decimals.
 */
export function parseMoney(input: string): number | null {
  const s = input.replace(/\s/g, '').replace(',', '.')
  if (!/^\d+(\.\d{1,2})?$/.test(s)) return null
  const [whole, frac = ''] = s.split('.')
  const cents = Number(whole) * 100 + Number(frac.padEnd(2, '0'))
  return cents > 0 ? cents : null
}

export const ACCOUNT_NUMBER_RE = /^DB-\d{4}-\d{4}-\d{4}$/i

/** Client-side check for an account number typed by the user; returns an error message or null. */
export function validateAccountNumber(input: string, own?: string): string | null {
  const v = input.trim()
  if (!v) return t('validation.recipientRequired')
  if (!ACCOUNT_NUMBER_RE.test(v)) return t('validation.recipientFormat')
  if (own && v.toUpperCase() === own.toUpperCase()) return t('validation.recipientSelf')
  return null
}

/** Client-side check for an amount; returns { cents } or { error }. */
export type AmountCheck = { ok: true; cents: number } | { ok: false; error: string }

export function validateAmount(input: string, maxCents?: number): AmountCheck {
  if (!input.trim()) return { ok: false, error: t('validation.amountRequired') }
  const cents = parseMoney(input)
  if (cents === null) return { ok: false, error: t('validation.amountFormat') }
  if (maxCents !== undefined && cents > maxCents) return { ok: false, error: t('validation.amountTooHigh', { balance: formatMoney(maxCents) }) }
  return { ok: true, cents }
}

const dateFmt = new Intl.DateTimeFormat(intlLocale, { day: 'numeric', month: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' })
export function formatDate(iso: string): string {
  return dateFmt.format(new Date(iso))
}

/** 250 bps -> "2,5 %" in the app locale. */
export function formatRate(rateBps: number): string {
  return new Intl.NumberFormat(intlLocale, { style: 'percent', minimumFractionDigits: 0, maximumFractionDigits: 2 }).format(rateBps / 10_000)
}

/** "2,5" / "2.5" / "3" -> 250 / 250 / 300 bps; null if not a valid 0–100 % with ≤2 decimals. */
export function parseRate(input: string): number | null {
  const s = input.replace(/\s|%/g, '').replace(',', '.')
  if (!/^\d+(\.\d{1,2})?$/.test(s)) return null
  const [whole, frac = ''] = s.split('.')
  const bps = Number(whole) * 100 + Number(frac.padEnd(2, '0'))
  return bps >= 0 && bps <= 10_000 ? bps : null
}
