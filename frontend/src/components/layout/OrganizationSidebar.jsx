import { ExternalLink } from 'lucide-react'
import { Link, NavLink } from 'react-router-dom'
import { UserSessionCard } from '../../features/auth/components/UserSessionCard.jsx'
import { ownerNav } from '../../features/organizations/lib/roleNav.js'
import { cn } from '../../utils/cn.js'
import { Brand } from './Brand.jsx'

export function OrganizationSidebar({ organization, onNavigate }) {
  return (
    <div className="flex h-full flex-col">
      <div className="px-5 pb-4 pt-6">
        <Brand onNavigate={onNavigate} />
      </div>

      <div className="mx-4 mb-4 rounded-xl border border-brand/15 bg-brand-soft/60 px-3 py-2.5">
        <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-brand-strong/70">
          Managing
        </p>
        <p className="mt-1 truncate text-sm font-semibold text-ink">
          {organization?.name || 'Your organization'}
        </p>
      </div>

      <nav aria-label="Primary" className="flex-1 px-3 py-2">
        <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[0.18em] text-muted/80">
          HostelOS Manage
        </p>
        <ul className="space-y-1">
          {ownerNav.map(({ to, label, icon: Icon, end }) => (
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
                        'size-4 shrink-0',
                        isActive
                          ? 'text-white'
                          : 'text-muted group-hover:text-brand',
                      )}
                      strokeWidth={1.9}
                      aria-hidden="true"
                    />
                    <span className="truncate">{label}</span>
                  </>
                )}
              </NavLink>
            </li>
          ))}
        </ul>

        <div className="mt-4 border-t border-line/70 pt-3">
          <Link
            to="/discover"
            onClick={onNavigate}
            className="flex items-center gap-2 rounded-xl px-3 py-2 text-xs font-medium text-muted transition-colors hover:bg-canvas hover:text-brand"
          >
            <ExternalLink className="size-3.5" aria-hidden="true" />
            View public listings
          </Link>
        </div>
      </nav>

      <div className="p-4">
        <UserSessionCard />
      </div>
    </div>
  )
}
