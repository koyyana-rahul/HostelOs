import { NavLink } from 'react-router-dom'
import { UserSessionCard } from '../../features/auth/components/UserSessionCard.jsx'
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
        <UserSessionCard />
      </div>
    </div>
  )
}
