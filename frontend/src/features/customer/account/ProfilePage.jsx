import { Card } from '../../../components/ui/Card.jsx'
import { Badge } from '../../../components/ui/Badge.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'

function DetailRow({ label, value }) {
  return (
    <div className="flex flex-col gap-1 border-b border-line/70 py-3 last:border-b-0 sm:flex-row sm:items-center sm:justify-between">
      <dt className="text-sm text-muted">{label}</dt>
      <dd className="text-sm font-medium text-ink">{value || '—'}</dd>
    </div>
  )
}

export default function AccountProfilePage() {
  useDocumentTitle('My profile')
  const user = useAppSelector(selectCurrentUser)

  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-xl font-semibold tracking-[-0.02em] text-ink">
          My profile
        </h2>
        <p className="mt-1 text-sm text-muted">
          Account details are managed by HostelOS authentication.
        </p>
      </header>

      <Card className="p-6">
        <dl>
          <DetailRow label="Display name" value={user?.displayName} />
          <DetailRow label="Login identifier" value={user?.loginIdentifier} />
          <DetailRow
            label="User ID"
            value={user?.id !== undefined ? String(user.id) : ''}
          />
        </dl>
      </Card>

      <Card className="p-6">
        <h3 className="text-sm font-semibold text-ink">Roles</h3>
        <ul className="mt-3 flex flex-wrap gap-2">
          {(user?.roles || []).length > 0 ? (
            user.roles.map((role) => (
              <li key={role}>
                <Badge tone="brand">{role}</Badge>
              </li>
            ))
          ) : (
            <li>
              <Badge>No roles assigned</Badge>
            </li>
          )}
        </ul>
      </Card>
    </div>
  )
}
