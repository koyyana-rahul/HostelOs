import { Outlet, NavLink } from 'react-router-dom'
import { Briefcase, ShieldCheck, User } from 'lucide-react'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { cn } from '../../../utils/cn.js'

const accountLinks = [
  { to: '/account/profile', label: 'My profile', icon: User },
  { to: '/my-enquiries', label: 'My enquiries', icon: Briefcase },
  { to: '/account/settings', label: 'Account settings', icon: ShieldCheck },
]

export default function AccountPage() {
  useDocumentTitle('My account')
  const user = useAppSelector(selectCurrentUser)

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <header>
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">
          My account
        </p>
        <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink">
          {user?.displayName || 'My account'}
        </h1>
        <p className="mt-1 text-sm text-muted">{user?.loginIdentifier}</p>
      </header>

      <div className="mt-8 grid gap-8 lg:grid-cols-[220px_minmax(0,1fr)]">
        <nav aria-label="Account" className="lg:sticky lg:top-24 lg:self-start">
          <ul className="flex gap-2 overflow-x-auto lg:flex-col">
            {accountLinks.map(({ to, label, icon: Icon }) => (
              <li key={to}>
                <NavLink
                  to={to}
                  end
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-2 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-brand text-white'
                        : 'text-muted hover:bg-canvas hover:text-ink',
                    )
                  }
                >
                  <Icon className="size-4" aria-hidden="true" />
                  {label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <div className="min-w-0">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
