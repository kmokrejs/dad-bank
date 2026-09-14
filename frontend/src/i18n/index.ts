import en from './en.json'
import cs from './cs.json'

/**
 * Locale is fixed at build/start time via VITE_LOCALE (see .env / .env.local).
 * No runtime switching for now — keeps every component a plain function of its props.
 */
export const LOCALES = { en, cs } as const
export type Locale = keyof typeof LOCALES

const envLocale = (import.meta.env.VITE_LOCALE as string | undefined)?.toLowerCase()
export const locale: Locale = envLocale && envLocale in LOCALES ? (envLocale as Locale) : 'cs'

/** BCP-47 tags for Intl (money, dates, percentages). Add an entry when you add a locale. */
const INTL_TAGS: Record<Locale, string> = { en: 'en-GB', cs: 'cs-CZ' }
export const intlLocale = INTL_TAGS[locale]

type Messages = typeof en
const messages: Messages = LOCALES[locale]

// "a.b.c" keys derived from the JSON shape, so typos fail at compile time.
type Leaves<T, P extends string = ''> = {
  [K in keyof T & string]: T[K] extends string ? `${P}${K}` : Leaves<T[K], `${P}${K}.`>
}[keyof T & string]
export type MessageKey = Leaves<Messages>

export type Params = Record<string, string | number>

/** Look up a message and substitute {placeholders}. Falls back to the key so a missing string is visible, not silent. */
export function t(key: MessageKey, params?: Params): string {
  const value = key.split('.').reduce<unknown>((acc, part) => (acc as Record<string, unknown> | undefined)?.[part], messages)
  if (typeof value !== 'string') return key
  if (!params) return value
  return value.replace(/\{(\w+)\}/g, (_, name: string) => (name in params ? String(params[name]) : `{${name}}`))
}
