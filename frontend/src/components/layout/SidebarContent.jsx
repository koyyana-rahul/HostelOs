import { CheckCircle2 } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { env } from '../../lib/env.js'
import { cn } from '../../utils/cn.js'
import { Brand } from './Brand.jsx'
import { navigation } from './navigation.js'

export function SidebarContent({ onNavigate }) {
  return (
    <div className="flex h-full flex-col">
      <div className="px-5 pb-6 pt-6">
        <Brand onNavigate={onNavigate} />
      </div>

      <nav aria-label="Primary" className="flex-1 px-3 py-2">
        <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[0.18em] text-muted/80">
          Workspace
        </p>
        <ul className="space-y-1">
          {navigation.map(({ to, label, icon: Icon, end }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={end}
                onClick={onNavigate}
                className={({ isActive }) =>
                  cn(
                    'group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-brand text-white shadow-sm'
                      : 'text-muted hover:bg-canvas hover:text-ink',
                  )
                }
              >
                {({ isActive }) => (
                  <>
                    <Icon
                      className={cn(
                        'size-4',
                        isActive
                          ? 'text-white'
                          : 'text-muted group-hover:text-brand',
                      )}
                      strokeWidth={1.9}
                      aria-hidden="true"
                    />
                    <span>{label}</span>
                  </>
                )}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <div className="p-4">
        <div className="rounded-2xl border border-line bg-canvas/70 p-4">
          <div className="flex items-center justify-between gap-2">
            <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted">
              Foundation
            </p>
            <span className="text-[11px] font-medium text-muted">
              v{env.appVersion}
            </span>
          </div>
          <div className="mt-3 flex items-center gap-2 text-sm font-medium text-ink">
            <CheckCircle2 className="size-4 text-success" aria-hidden="true" />
            <span>Frontend ready</span>
          </div>
          <p className="mt-1.5 text-xs leading-5 text-muted">
            Core tooling is configured. Feature modules can be added safely.
          </p>
        </div>
      </div>
    </div>
  )
}
