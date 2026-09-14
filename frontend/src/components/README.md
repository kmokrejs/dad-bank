# UI conventions

- **Tokens** live in `src/styles/tokens.css` — colors, type scale, spacing, radii. Change the look there, not in components.
- **Base styles + tiny utilities** (`.stack`, `.row`, `.muted`, `.mono`, …) in `src/styles/base.css`. Keep the utility list short.
- **Components** in `components/ui/` — one `.tsx` + one `.css` per component, BEM-style class names (`.card`, `.card__header`, `.btn--primary`).
  Import from `components/ui` (barrel). Add a new component here when the same markup+style shows up twice.
- **Layouts** in `components/layout/` — `AppShell` for signed-in pages, `AuthLayout` for login/register.
- **Data** goes through React Query hooks in `features/<area>/queries.ts`; pages never call `fetch` directly.
- Money is integer cents; format with `formatMoney()` only at render time.
- **Text** never lives in components: every user-visible string goes through `t('section.key')` from `src/i18n`, with matching entries in `en.json` and `cs.json`.
- **Kid-friendly touches** are opt-in props, not separate components: `Stat icon="🪙" big`, `Card icon="📒"`, `Card className="card--accent"`.
