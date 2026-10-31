import { ChevronRight, ShieldCheck, UserRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Badge } from '../../../components/ui/Badge.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { useOwnerOrganization } from '../../organizations/hooks/useOwnerOrganization.js'

const links = [
  {
    to: '/app/account/profile',
    icon: UserRound,
    title: 'Profile',
    description: 'View the identity attached to this session.',
  },
  {
    to: '/app/account/settings',
    icon: ShieldCheck,
    title: 'Account settings',
    description: 'Review your session and sign out.',
  },
]

export default function OrgAccountPage() {
  useDocumentTitle('Account')
  const user = useAppSelector(selectCurrentUser)
  const { organization } = useOwnerOrganization()

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">
          Account
        </h1>
        <p className="mt-1 text-sm text-muted">
          Personal account details for the owner workspace.
        </p>
      </header>

      <Card className="p-6">
        <div className="flex items-center gap-4">
          <span className="grid size-14 place-items-center rounded-2xl bg-brand text-lg font-semibold text-white">
            {(user?.displayName || user?.loginIdentifier || 'H')
              .trim()
              .charAt(0)
              .toUpperCase()}
          </span>
          <div className="min-w-0">
            <p className="truncate text-lg font-semibold text-ink">
              {user?.displayName || 'HostelOS user'}
            </p>
            <p className="truncate text-sm text-muted">
              {user?.loginIdentifier}
            </p>
          </div>
        </div>
        <div className="mt-5 flex flex-wrap gap-2">
          {(user?.roles || []).map((role) => (
            <Badge key={role} tone="brand">
              {role}
            </Badge>
          ))}
        </div>
        {organization ? (
          <p className="mt-4 text-xs text-muted">
            Managing {organization.name} ({organization.slug})
          </p>
        ) : null}
      </Card>

      <ul className="grid gap-4 sm:grid-cols-2">
        {links.map(({ to, icon: Icon, title, description }) => (
          <li key={to}>
            <Link to={to} className="block">
              <Card className="flex items-center gap-4 p-5 transition-colors hover:border-brand/30">
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-soft text-brand">
                  <Icon className="size-5" aria-hidden="true" />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block text-sm font-semibold text-ink">
                    {title}
                  </span>
                  <span className="mt-0.5 block text-xs text-muted">
                    {description}
                  </span>
                </span>
                <ChevronRight className="size-4 text-muted" aria-hidden="true" />
              </Card>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
