import { Link } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { formatDate, titleCase } from '../../../utils/format.js'
import { useListMyEnquiriesQuery } from '../api/customerApi.js'

export default function MyEnquiriesPage() {
  useDocumentTitle('My enquiries')
  const { data = [], isLoading, isError, error, refetch } =
    useListMyEnquiriesQuery()

  const enquiries = Array.isArray(data) ? data : data?.content || []

  return (
    <div className="space-y-6">
      <header>
        <h2 className="text-xl font-semibold tracking-[-0.02em] text-ink">
          My enquiries
        </h2>
        <p className="mt-1 text-sm text-muted">
          Conversations you have started with HostelOS owners.
        </p>
      </header>

      {isError ? (
        <ErrorState
          title="Could not load enquiries"
          description={
            error?.data?.message ||
            'An unexpected error interrupted this view. Try again in a moment.'
          }
          action={
            <Button onClick={() => refetch()} variant="secondary">
              Retry
            </Button>
          }
        />
      ) : isLoading ? (
        <div className="flex h-40 items-center justify-center">
          <LoaderCircle className="size-5 animate-spin text-muted" aria-hidden="true" />
        </div>
      ) : enquiries.length === 0 ? (
        <Card className="p-8 text-center">
          <p className="font-medium text-ink">No enquiries yet</p>
          <p className="mt-1 text-sm text-muted">
            Enquire about a listing and it will appear here.
          </p>
          <div className="mt-4">
            <Button as={Link} to="/discover">
              Browse listings
            </Button>
          </div>
        </Card>
      ) : (
        <ul className="space-y-3">
          {enquiries.map((enquiry) => (
            <li key={enquiry.id}>
              <Card className="p-5">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-sm font-semibold text-ink">
                    <Link
                      to={`/my-enquiries/${enquiry.id}`}
                      className="hover:text-brand hover:underline"
                    >
                      {enquiry.listingTitle}
                    </Link>
                  </p>
                  <span className="text-xs text-muted">
                    {titleCase(enquiry.status || 'NEW')}
                  </span>
                </div>
                <p className="mt-2 whitespace-pre-line text-sm text-muted">
                  {enquiry.message}
                </p>
                <p className="mt-2 text-xs text-muted">
                  {formatDate(enquiry.createdAt)}
                </p>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
