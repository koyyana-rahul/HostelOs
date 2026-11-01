import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { Menu, X, LayoutDashboard, Building2, Search, Briefcase, ShieldCheck, User, LogOut } from 'lucide-react'
import { Brand } from '../Brand.jsx'
import { Button } from '../../ui/Button.jsx'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../../features/auth/authSlice.js'
import { useNavigate } from 'react-router-dom'
import { hasPlatformRole, hasOrganizationRole, isCustomerOnly } from '../../../features/auth/lib/roleResolver.js'

const publicNavLinks = [
  { to: '/', label: 'Home' },
  { to: '/discover', label: 'Discover' },
  { to: '/for-owners', label: 'For Owners' },
  { to: '/features', label: 'Features' },
  { to: '/about', label: 'About' },
  { to: '/contact', label: 'Contact' },
]

const customerNavLinks = [
  { to: '/discover', label: 'Discover', icon: Search },
  { to: '/my-enquiries', label: 'My Enquiries', icon: Briefcase },
  { to: '/about', label: 'About' },
  { to: '/contact', label: 'Contact' },
]

function AuthActions({ user, onNavigate }) {
  if (!user) {
    return (
      <div className="flex items-center gap-2">
        <Button as={Link} to="/login" variant="ghost" size="sm" onClick={onNavigate}>
          Sign in
        </Button>
        <Button as={Link} to="/register" size="sm" variant="primary" onClick={onNavigate}>
          Get started
        </Button>
      </div>
    )
  }

  if (hasPlatformRole(user)) {
    return (
      <div className="flex items-center gap-2">
        <Button as={Link} to="/platform" variant="primary" size="sm" onClick={onNavigate}>
          Platform
        </Button>
      </div>
    )
  }

  if (hasOrganizationRole(user)) {
    return (
      <div className="flex items-center gap-2">
        <Button as={Link} to="/app" variant="primary" size="sm" onClick={onNavigate}>
          Dashboard
        </Button>
      </div>
    )
  }

  if (isCustomerOnly(user)) {
    return (
      <div className="flex items-center gap-2">
        <Button as={Link} to="/discover" variant="secondary" size="sm" onClick={onNavigate}>
          Browse
        </Button>
        <Button as={Link} to="/account" variant="ghost" size="sm" onClick={onNavigate}>
          My Account
        </Button>
      </div>
    )
  }

  return null
}

export function PublicHeader() {
  const [open, setOpen] = useState(false)
  const user = useAppSelector(selectCurrentUser)
  const navigate = useNavigate()

  const isAuthed = !!user
  const isPlatform = hasPlatformRole(user)
  const isOwner = hasOrganizationRole(user)
  const isCustomer = isCustomerOnly(user)

  let primaryNav = publicNavLinks
  if (isPlatform) {
    primaryNav = [
      { to: '/platform', label: 'Platform Dashboard', icon: ShieldCheck }
    ]
  } else if (isOwner) {
    primaryNav = [
      { to: '/app', label: 'Owner Dashboard', icon: LayoutDashboard },
      { to: '/app/properties', label: 'Properties', icon: Building2 },
      { to: '/app/listings', label: 'Listings', icon: Search },
      { to: '/app/enquiries', label: 'Enquiries', icon: Briefcase },
      { to: '/discover', label: 'Discover', icon: Search },
    ]
  } else if (isAuthed && isCustomer) {
    primaryNav = customerNavLinks
  }

  const closeMenu = () => setOpen(false)

  return (
    <header className="sticky top-0 z-40 border-b border-line/80 bg-surface/95 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8">
        <Brand />
        <nav className="hidden items-center gap-6 md:flex">
          {primaryNav.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex items-center gap-1.5 text-sm font-medium transition-colors ${
                  isActive ? 'text-brand' : 'text-muted hover:text-ink'
                }`
              }
              end={to === '/'}
            >
              {Icon ? <Icon className="size-4" aria-hidden="true" /> : null}
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="hidden items-center gap-2 md:flex">
          <AuthActions user={user} onNavigate={closeMenu} />
        </div>
        <button
          className="inline-flex items-center justify-center rounded-lg p-2 text-muted hover:text-ink md:hidden"
          onClick={() => setOpen((v) => !v)}
          aria-label="Toggle navigation"
          aria-expanded={open}
        >
          {open ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </div>

      {open ? (
        <div className="border-t border-line/80 md:hidden">
          <nav className="mx-auto flex max-w-7xl flex-col px-4 py-2 sm:px-6 lg:px-8">
            {primaryNav.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                onClick={closeMenu}
                className={({ isActive }) =>
                  `flex items-center gap-2 py-2 text-sm font-medium ${
                    isActive ? 'text-brand' : 'text-muted'
                  }`
                }
                end={to === '/'}
              >
                {Icon ? <Icon className="size-4" aria-hidden="true" /> : null}
                {label}
              </NavLink>
            ))}
            <div className="mt-2 flex items-center gap-2 pb-2">
              <AuthActions user={user} onNavigate={closeMenu} />
            </div>
          </nav>
        </div>
      ) : null}
    </header>
  )
}

export default PublicHeader