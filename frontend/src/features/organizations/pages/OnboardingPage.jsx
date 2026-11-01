import { Building2, CheckCircle2, Plus } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { useListMyOrganizationsQuery } from '../api/organizationApi.js'

const setupSteps = [
  'Create your organization workspace',
  'Add your hostel or PG properties',
  'Publish listings for customers to discover',
  'Respond to customer enquiries in one inbox',
]

export default function OnboardingPage() {
  useDocumentTitle('Onboarding')
  const { data = [] } = useListMyOrganizationsQuery()
  const organizations = Array.isArray(data) ? data : []

  return (
    <main className="min-h-dvh bg-canvas">
      <div className="mx-auto w-full max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="mb-8">
          <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">
            Owner onboarding
          </p>
          <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink">
            Get started with HostelOS
          </h1>
          <p className="mt-2 text-sm leading-6 text-muted">
            Create your organization to unlock the HostelOS Manage workspace.
          </p>
        </div>

        <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <Card className="p-6 sm:p-8">
            <span className="grid size-12 place-items-center rounded-xl bg-brand-soft">
              <Building2 className="size-5 text-brand" aria-hidden="true" />
            </span>
            <h2 className="mt-4 text-lg font-semibold text-ink">
              {organizations.length > 0
                ? 'Continue to your workspace'
                : 'Create your organization'}
            </h2>
            <p className="mt-2 text-sm leading-6 text-muted">
              {organizations.length > 0
                ? 'Your organization is ready. Continue to the owner dashboard to manage properties, listings, and enquiries.'
                : 'Set up your organization name and URL slug. You can add properties and listings right after.'}
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              {organizations.length > 0 ? (
                <Button as={Link} to="/app/overview">
                  Open workspace
                </Button>
              ) : (
                <Button as={Link} to="/onboarding/create">
                  <Plus className="size-4" aria-hidden="true" />
                  Create organization
                </Button>
              )}
              {organizations.length > 0 ? (
                <Button as={Link} to="/onboarding/create" variant="secondary">
                  Create another
                </Button>
              ) : null}
            </div>
          </Card>

          <Card className="p-6">
            <h2 className="text-sm font-semibold text-ink">What happens next</h2>
            <ul className="mt-4 space-y-3">
              {setupSteps.map((step) => (
                <li key={step} className="flex items-start gap-2.5 text-sm text-muted">
                  <CheckCircle2
                    className="mt-0.5 size-4 shrink-0 text-brand"
                    aria-hidden="true"
                  />
                  {step}
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </div>
    </main>
  )
}
