import {
  Briefcase,
  Building2,
  CheckCircle2,
  FileText,
  Search,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { LoaderCircle } from 'lucide-react'
import { Badge } from '../../../components/ui/Badge.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { useGetOrganizationDashboardQuery } from '../../organizations/api/organizationApi.js'
import { useOwnerOrganization } from '../../organizations/hooks/useOwnerOrganization.js'

function Metric({ label, value, hint, icon: Icon }) {
  return (
    <Card className="p-5">
      <span className="grid size-10 place-items-center rounded-xl bg-brand-soft text-brand">
        <Icon className="size-5" aria-hidden="true" />
      </span>
      <p className="mt-4 text-xs font-medium text-muted">{label}</p>
      <p className="mt-1 text-2xl font-semibold tracking-tight text-ink">
        {value}
      </p>
      {hint ? <p className="mt-1 text-xs text-muted">{hint}</p> : null}
    </Card>
  )
}

export default function OrgOverviewPage() {
  useDocumentTitle('Overview')
  const { organization, organizationId } = useOwnerOrganization()

  const {
    data: dashboard,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useGetOrganizationDashboardQuery(organizationId, {
    skip: !organizationId,
  })

  if (isError) {
    return (
      <ErrorState
        title="Could not load overview"
        description={
          error?.data?.message ||
          'An unexpected error interrupted this view. Try again in a moment.'
        }
        action={
          <Button onClick={() => refetch()} disabled={isFetching}>
            Retry
          </Button>
        }
      />
    )
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">
            Overview
          </h1>
          <p className="mt-1 text-sm text-muted">
            {organization?.name || 'Organization overview'}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button as={Link} to="/app/listings" variant="secondary" size="sm">
            <Search className="size-4" aria-hidden="true" />
            Manage listings
          </Button>
          <Button as={Link} to="/app/enquiries" size="sm">
            <Briefcase className="size-4" aria-hidden="true" />
            View enquiries
          </Button>
        </div>
      </header>

      {isLoading || !dashboard ? (
        <div className="flex h-48 items-center justify-center">
          <LoaderCircle className="size-5 animate-spin text-muted" aria-hidden="true" />
        </div>
      ) : (
        <>
          <section aria-label="Dashboard metrics">
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <Metric
                icon={Building2}
                label="Properties"
                value={dashboard.propertyCount}
                hint={`${dashboard.activePropertyCount} active`}
              />
              <Metric
                icon={FileText}
                label="Listings"
                value={dashboard.listingCount}
                hint={`${dashboard.publishedListingCount} published`}
              />
              <Metric
                icon={Search}
                label="Drafts"
                value={dashboard.draftListingCount}
                hint="Ready to finish and publish"
              />
              <Metric
                icon={Briefcase}
                label="Enquiries"
                value={dashboard.enquiryCount}
                hint={`${dashboard.newEnquiryCount} new`}
              />
            </div>
          </section>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card className="p-6">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-base font-semibold text-ink">
                    Listing status
                  </h2>
                  <p className="mt-1 text-xs text-muted">
                    What customers can see right now.
                  </p>
                </div>
                <Badge tone="brand">
                  {dashboard.publishedListingCount} live
                </Badge>
              </div>
              <dl className="mt-5 grid grid-cols-3 gap-3 text-center">
                <div className="rounded-xl bg-canvas px-3 py-4">
                  <dt className="text-xs text-muted">Published</dt>
                  <dd className="mt-1 text-xl font-semibold text-ink">
                    {dashboard.publishedListingCount}
                  </dd>
                </div>
                <div className="rounded-xl bg-canvas px-3 py-4">
                  <dt className="text-xs text-muted">Draft</dt>
                  <dd className="mt-1 text-xl font-semibold text-ink">
                    {dashboard.draftListingCount}
                  </dd>
                </div>
                <div className="rounded-xl bg-canvas px-3 py-4">
                  <dt className="text-xs text-muted">Archived</dt>
                  <dd className="mt-1 text-xl font-semibold text-ink">
                    {dashboard.archivedListingCount}
                  </dd>
                </div>
              </dl>
            </Card>

            <Card className="p-6">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-base font-semibold text-ink">
                    Enquiry pipeline
                  </h2>
                  <p className="mt-1 text-xs text-muted">
                    Follow up on customer conversations.
                  </p>
                </div>
                <Badge tone="warning">
                  {dashboard.newEnquiryCount} new
                </Badge>
              </div>
              <ul className="mt-5 space-y-3">
                <li className="flex items-center justify-between text-sm">
                  <span className="text-muted">New</span>
                  <span className="font-semibold text-ink">
                    {dashboard.newEnquiryCount}
                  </span>
                </li>
                <li className="flex items-center justify-between text-sm">
                  <span className="text-muted">Read</span>
                  <span className="font-semibold text-ink">
                    {dashboard.readEnquiryCount}
                  </span>
                </li>
                <li className="flex items-center justify-between text-sm">
                  <span className="text-muted">Responded</span>
                  <span className="font-semibold text-ink">
                    {dashboard.respondedEnquiryCount}
                  </span>
                </li>
                <li className="flex items-center justify-between text-sm">
                  <span className="text-muted">Closed</span>
                  <span className="font-semibold text-ink">
                    {dashboard.closedEnquiryCount}
                  </span>
                </li>
              </ul>
            </Card>
          </div>

          {dashboard.listingCount === 0 ? (
            <Card className="p-6">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-start gap-3.5">
                  <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-soft text-brand">
                    <CheckCircle2 className="size-5" aria-hidden="true" />
                  </span>
                  <div>
                    <h2 className="text-base font-semibold text-ink">
                      Create your first listing
                    </h2>
                    <p className="mt-1 text-xs text-muted">
                      Add a hostel, PG, or room. Draft and publish when ready.
                    </p>
                  </div>
                </div>
                <Button as={Link} to="/app/listings" size="sm">
                  Go to listings
                </Button>
              </div>
            </Card>
          ) : null}
        </>
      )}
    </div>
  )
}
