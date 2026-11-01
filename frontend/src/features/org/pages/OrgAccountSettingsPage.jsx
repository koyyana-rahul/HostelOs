import { LoaderCircle, LogOut, ShieldCheck } from 'lucide-react'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { useLogout } from '../../auth/hooks/useLogout.js'

export default function OrgAccountSettingsPage() {
  useDocumentTitle('Account settings')
  const { signOut, isSigningOut, error } = useLogout()

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">
          Account settings
        </h1>
        <p className="mt-1 text-sm text-muted">
          Security and session controls for your account.
        </p>
      </header>

      <Card className="max-w-2xl p-6">
        <div className="flex items-start gap-3">
          <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-soft text-brand">
            <ShieldCheck className="size-5" aria-hidden="true" />
          </span>
          <div>
            <h2 className="text-base font-semibold text-ink">
              Session authentication
            </h2>
            <p className="mt-1 text-sm leading-6 text-muted">
              Your session is protected with an HTTP-only cookie and
              CSRF-protected requests. Signing out clears the session on this
              device.
            </p>
          </div>
        </div>

        {error ? (
          <p role="alert" className="mt-4 text-sm text-danger">
            {error}
          </p>
        ) : null}

        <div className="mt-5 border-t border-line/70 pt-5">
          <Button
            variant="secondary"
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
        </div>
      </Card>
    </div>
  )
}
