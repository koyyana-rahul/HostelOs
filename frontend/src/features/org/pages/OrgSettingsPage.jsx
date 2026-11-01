import { useState } from 'react'
import { AlertCircle, CheckCircle2, LoaderCircle } from 'lucide-react'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { useUpdateOrganizationMutation } from '../../organizations/api/organizationApi.js'
import { useOwnerOrganization } from '../../organizations/hooks/useOwnerOrganization.js'

const inputClassName =
  'h-11 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink transition-colors placeholder:text-muted/70 hover:border-brand/30 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15 disabled:cursor-not-allowed disabled:bg-canvas disabled:text-muted'

export default function OrgSettingsPage() {
  useDocumentTitle('Organization settings')
  const { organization } = useOwnerOrganization()
  const [updateOrganization, { isLoading }] = useUpdateOrganizationMutation()
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    const values = Object.fromEntries(new FormData(event.target).entries())
    const name = values.name.trim()

    if (!name) {
      setError('Organization name is required.')
      return
    }

    setError(null)
    setSaved(false)

    try {
      await updateOrganization({
        organizationId: organization.id,
        name,
      }).unwrap()
      setSaved(true)
    } catch (requestError) {
      setError(
        requestError?.data?.message ||
          'Could not update the organization. Try again.',
      )
    }
  }

  if (!organization) {
    return (
      <div className="flex h-48 items-center justify-center">
        <LoaderCircle className="size-5 animate-spin text-muted" aria-hidden="true" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">
          Settings
        </h1>
        <p className="mt-1 text-sm text-muted">
          Manage the organization shown to customers.
        </p>
      </header>

      <Card className="max-w-2xl p-6">
        <h2 className="text-base font-semibold text-ink">
          Organization profile
        </h2>
        <p className="mt-1 text-xs text-muted">
          Your organization name appears on every published listing.
        </p>

        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          {error ? (
            <div
              role="alert"
              className="flex items-start gap-2.5 rounded-xl border border-danger/15 bg-danger-soft/60 p-3"
            >
              <AlertCircle className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden="true" />
              <p className="text-sm text-ink">{error}</p>
            </div>
          ) : null}

          {saved ? (
            <div
              role="status"
              className="flex items-start gap-2.5 rounded-xl border border-success/15 bg-success-soft/60 p-3"
            >
              <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-success" aria-hidden="true" />
              <p className="text-sm text-ink">Organization updated.</p>
            </div>
          ) : null}

          <label className="block">
            <span className="text-sm font-medium text-ink">Name</span>
            <input
              key={organization.name}
              name="name"
              required
              maxLength={120}
              defaultValue={organization.name}
              className={`mt-2 ${inputClassName}`}
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Slug</span>
            <input
              value={organization.slug}
              readOnly
              disabled
              className={`mt-2 ${inputClassName}`}
            />
            <span className="mt-1.5 block text-xs text-muted">
              The organization slug is permanent.
            </span>
          </label>

          <div className="flex justify-end">
            <Button type="submit" disabled={isLoading}>
              {isLoading ? (
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
              ) : null}
              Save changes
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
