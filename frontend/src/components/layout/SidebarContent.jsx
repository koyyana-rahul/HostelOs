import { NavLink, useLocation } from 'react-router-dom'
import { UserSessionCard } from '../../features/auth/components/UserSessionCard.jsx'
import { cn } from '../../utils/cn.js'
import { Brand } from './Brand.jsx'
import { navigation } from './navigation.js'
import { useAppSelector } from '../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../features/auth/authSlice.js'
import { platformNav, orgNav, staffNav } from '../../features/organizations/lib/roleNav.js'
import { useGetMembershipsQuery } from '../../features/auth/api/authApi.js'
import { selectCurrentOrganizationId } from '../../features/organizations/organizationSlice.js'
import { hasPlatformRole } from '../../features/auth/lib/roleResolver.js'

export function SidebarContent({ onNavigate }) {
  const location = useLocation()
  const user = useAppSelector(selectCurrentUser)
  const { data: memberships = [] } = useGetMembershipsQuery()
  const currentOrgId = useAppSelector(selectCurrentOrganizationId)
  const isPlatform = location.pathname.startsWith('/platform')
  const isApp = location.pathname.startsWith('/app')

  const isPlatformAdmin = hasPlatformRole(user)
  const activeMemberships = memberships.filter((m) => m.status === 'ACTIVE')
  const currentMembership =
    activeMemberships.find((m) => m.organizationId === currentOrgId) || activeMemberships[0]

  let navItems = navigation
  if (isPlatformAdmin && isPlatform) {
    navItems = platformNav
  } else if (isApp && currentMembership) {
    if (currentMembership.orgRole === 'ORG_ADMIN') {
      navItems = orgNav
    } else if (currentMembership.orgRole === 'ORG_STAFF') {
      navItems = staffNav
    } else {
      navItems = staffNav
    }
  }

  return (
    <div className="flex h-full flex-col">
      <div className="px-5 pb-6 pt-6">
        <Brand onNavigate={onNavigate} />
      </div>

      <nav aria-label="Primary" className="flex-1 px-3 py-2">
        <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[0.18em] text-muted/80">
          {isPlatformAdmin && isPlatform ? 'Platform' : isApp ? 'Organization' : 'Workspace'}
        </p>
        <ul className="space-y-1">
          {navItems.map(({ to, label, icon: Icon, end }) => (
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
                    <span className="truncate" title={label}>
                      {label}
                    </span>
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
