import { LoaderCircle, LogOut } from 'lucide-react'
import { Badge } from '../../../components/ui/Badge.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../authSlice.js'
import { useLogout } from '../hooks/useLogout.js'
import { normalizeRoleNames } from '../lib/roles.js'

function getInitials(user) {
  const source = user.displayName?.trim() || user.loginIdentifier?.trim() || ''

  if (!source) {
    return 'H'
  }

  return source
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('')
}

export function UserSessionCard() {
  const user = useAppSelector(selectCurrentUser)
  const { signOut, isSigningOut, error } = useLogout()

  if (!user) {
    return null
  }

  const roles = normalizeRoleNames(user.roles)

  return (
    <div className="rounded-2xl border border-line bg-canvas/70 p-4">
      <div className="flex items-center gap-3">
        <span
          className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand text-sm font-semibold text-white shadow-sm"
          aria-hidden="true"
        >
          {getInitials(user)}
        </span>
        <div className="min-w-0">
          <p
            className="break-words text-sm font-semibold leading-5 text-ink"
            title={user.displayName}
          >
            {user.displayName}
          </p>
          <p
            className="break-all text-xs leading-5 text-muted"
            title={user.loginIdentifier}
          >
            {user.loginIdentifier}
          </p>
        </div>
      </div>

      <div className="mt-3">
        <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted/80">
          Roles
        </p>
        <ul
          aria-label="Server-assigned roles"
          className="mt-2 flex flex-wrap gap-1.5"
        >
          {roles.length > 0 ? (
            roles.map((role) => (
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
        <p className="mt-2 text-[11px] leading-4 text-muted">
          Informational only; access is enforced by the backend.
        </p>
      </div>

      <Button
        variant="secondary"
        size="sm"
        className="mt-4 w-full"
        onClick={signOut}
        disabled={isSigningOut}
      >
        {isSigningOut ? (
          <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
        ) : (
          <LogOut className="size-4" aria-hidden="true" />
        )}
        {isSigningOut ? 'Signing out...' : 'Sign out'}
      </Button>

      {error ? (
        <p role="alert" className="mt-3 text-xs leading-5 text-danger">
          {error}
        </p>
      ) : null}
    </div>
  )
}
