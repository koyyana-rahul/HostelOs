import { Plus, Building2, LoaderCircle, AlertCircle } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Card } from '../../../components/ui/Card.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Badge } from '../../../components/ui/Badge.jsx'
import { useGetMembershipsQuery } from '../../auth/api/authApi.js'
import { useListOrganizationsQuery } from '../api/organizationApi.js'

export default function OnboardingPage() {
  const { data: memberships = [], isLoading: membershipsLoading } = useGetMembershipsQuery()
  const { data: organizations = [], isLoading: orgsLoading } = useListOrganizationsQuery(undefined, {
    skip: memberships.length === 0,
  })

  const activeMemberships = memberships.filter((m) => m.status === 'ACTIVE')
  const isLoading = membershipsLoading || (memberships.length > 0 && orgsLoading)

  return (
    <main className="min-h-dvh bg-canvas">
      <div className="mx-auto w-full max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="mb-8">
          <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">Onboarding</p>
          <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink">Get started with HostelOS</h1>
          <p className="mt-2 text-sm leading-6 text-muted">
            Create your organization or select from your existing memberships.
          </p>
        </div>

        {isLoading ? (
          <div className="flex h-48 items-center justify-center">
            <LoaderCircle className="size-5 animate-spin text-muted" aria-hidden="true" />
          </div>
        ) : null}

        {!isLoading && activeMemberships.length > 0 ? (
          <Card className="mb-6 p-6">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <h2 className="text-lg font-semibold text-ink">Your organizations</h2>
                <p className="mt-1 text-sm text-muted">Select an organization to continue</p>
              </div>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {activeMemberships.map((membership) => (
                <Link
                  key={membership.id}
                  to="/app"
                  className="flex items-center gap-3 rounded-xl border border-line p-4 transition-colors hover:border-brand/40 hover:bg-canvas"
                >
                  <span className="grid size-10 place-items-center rounded-xl bg-brand-soft">
                    <Building2 className="size-4 text-brand" aria-hidden="true" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium text-ink">{membership.organizationName}</p>
                    <p className="text-xs text-muted">{membership.orgRole}</p>
                  </div>
                  <Badge tone="success">Active</Badge>
                </Link>
              ))}
            </div>
          </Card>
        ) : null}

        {!isLoading && activeMemberships.length === 0 ? (
          <Card className="p-6">
            <div className="flex items-start gap-4">
              <span className="grid size-12 place-items-center rounded-xl bg-brand-soft">
                <AlertCircle className="size-5 text-brand" aria-hidden="true" />
              </span>
              <div className="flex-1">
                <h2 className="text-lg font-semibold text-ink">No organizations yet</h2>
                <p className="mt-1 text-sm leading-6 text-muted">
                  You don't have any active organization memberships. Create your first organization to get started.
                </p>
                <div className="mt-4">
                  <Button as={Link} to="/onboarding/create">
                    <Plus className="size-4" aria-hidden="true" />
                    Create organization
                  </Button>
                </div>
              </div>
            </div>
          </Card>
        ) : null}
      </div>
    </main>
  )
}
