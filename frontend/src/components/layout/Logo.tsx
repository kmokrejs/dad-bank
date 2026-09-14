export function Logo({ size = 24 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden>
      <rect x="2" y="2" width="20" height="20" rx="5" fill="var(--color-primary)" />
      <path d="M7 16.5V7.5h4.2c2.1 0 3.4 1.1 3.4 2.6 0 1-.6 1.7-1.4 2 1.1.3 1.8 1.1 1.8 2.3 0 1.6-1.3 2.6-3.6 2.6H7zm2.2-5.3h1.8c.9 0 1.4-.4 1.4-1.1 0-.7-.5-1.1-1.4-1.1H9.2v2.2zm0 3.7h2.1c1 0 1.5-.4 1.5-1.2 0-.7-.5-1.2-1.5-1.2H9.2v2.4z" fill="#fff" />
    </svg>
  )
}
