import { Building2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { env } from '../../lib/env.js'

export function Brand({ onNavigate }) {
  return (
    <Link
      to="/"
      onClick={onNavigate}
      className="group flex items-center gap-3"
    >
      <span className="grid size-10 place-items-center rounded-xl bg-brand text-white shadow-sm transition-colors group-hover:bg-brand-strong">
        <Building2 className="size-5" strokeWidth={1.8} aria-hidden="true" />
      </span>
      <span className="flex flex-col">
        <span className="text-base font-semibold tracking-tight text-ink">
          {env.appName}
        </span>
        <span className="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted">
          Operations
        </span>
      </span>
    </Link>
  )
}
