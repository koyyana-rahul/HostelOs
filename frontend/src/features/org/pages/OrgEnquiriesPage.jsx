import { useState } from 'react'
import {
  AlertCircle,
  CheckCircle2,
  LoaderCircle,
  Mail,
  Phone,
} from 'lucide-react'
import { Badge } from '../../../components/ui/Badge.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { formatDate, titleCase } from '../../../utils/format.js'
import {
  useListEnquiriesQuery,
  useUpdateEnquiryStatusMutation,
} from '../../enquiries/api/enquiryApi.js'
import { useOwnerOrganization } from '../../organizations/hooks/useOwnerOrganization.js'

const statusFilters = [
  { value: '', label: 'All enquiries' },
  { value: 'NEW', label: 'New' },
  { value: 'READ', label: 'Read' },
  { value: 'RESPONDED', label: 'Responded' },
  { value: 'CLOSED', label: 'Closed' },
]

const nextStatus = {
  NEW: 'READ',
  READ: 'RESPONDED',
  RESPONDED: 'CLOSED',
}

function statusTone(status) {
  if (status === 'NEW') return 'warning'
  if (status === 'READ') return 'brand'
  if (status === 'RESPONDED') return 'success'
  return 'neutral'
}

function actionLabel(status) {
  if (status === 'NEW') return 'Mark read'
  if (status === 'READ') return 'Mark responded'
  if (status === 'RESPONDED') return 'Close enquiry'
  return null
}

export default function OrgEnquiriesPage() {
  useDocumentTitle('Enquiries')
  const { organizationId } = useOwnerOrganization()
  const [statusFilter, setStatusFilter] = useState('')
  const [actionError, setActionError] = useState(null)
  const [updateStatus, { isLoading: isUpdating }] =
    useUpdateEnquiryStatusMutation()

  const {
    data: enquiries = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useListEnquiriesQuery(
    { organizationId, status: statusFilter || undefined },
    { skip: !organizationId },
  )

  const changeStatus = async (enquiry, status) => {
    setActionError(null)
    try {
      await updateStatus({
        organizationId,
        enquiryId: enquiry.id,
        status,
        version: enquiry.version,
      }).unwrap()
    } catch (requestError) {
      setActionError(
        requestError?.data?.message ||
          'Could not update the enquiry. Refresh and try again.',
      )
    }
  }

  if (isError) {
    return (
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
    )
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">
          Enquiries
        </h1>
        <p className="mt-1 text-sm text-muted">
          Follow up with customers interested in your listings.
        </p>
      </header>

      <div className="flex flex-wrap items-end justify-between gap-3">
        <label className="block">
          <span className="text-xs font-semibold text-ink">Filter</span>
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
            className="mt-1.5 h-10 rounded-lg border border-line bg-surface px-3 text-sm text-ink"
          >
            {statusFilters.map((filter) => (
              <option key={filter.value} value={filter.value}>
                {filter.label}
              </option>
            ))}
          </select>
        </label>
        <p className="text-xs text-muted">
          {enquiries.length} {enquiries.length === 1 ? 'enquiry' : 'enquiries'}
        </p>
      </div>

      {actionError ? (
        <div
          role="alert"
          className="flex items-start gap-2.5 rounded-xl border border-danger/15 bg-danger-soft/60 p-4"
        >
          <AlertCircle className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden="true" />
          <p className="text-sm text-ink">{actionError}</p>
        </div>
      ) : null}

      {isLoading ? (
        <div className="flex h-48 items-center justify-center">
          <LoaderCircle className="size-5 animate-spin text-muted" aria-hidden="true" />
        </div>
      ) : enquiries.length === 0 ? (
        <Card className="p-8 text-center">
          <p className="font-medium text-ink">
            {statusFilter ? 'No enquiries with this status' : 'No enquiries yet'}
          </p>
          <p className="mt-1 text-sm text-muted">
            Enquiries from customers will appear here as soon as they reach out.
          </p>
        </Card>
      ) : (
        <ul className="space-y-4">
          {enquiries.map((enquiry) => (
            <li key={enquiry.id}>
              <Card className="p-5">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-sm font-semibold text-ink">
                        {enquiry.customerName || 'Customer'}
                      </h2>
                      <Badge tone={statusTone(enquiry.status)}>
                        {titleCase(enquiry.status)}
                      </Badge>
                    </div>
                    <p className="mt-1 text-xs text-muted">
                      {enquiry.listingTitle} · {formatDate(enquiry.createdAt)}
                    </p>
                    <p className="mt-3 whitespace-pre-line rounded-xl bg-canvas p-3 text-sm leading-6 text-ink">
                      {enquiry.message}
                    </p>
                    <div className="mt-3 flex flex-wrap gap-4 text-xs text-muted">
                      {enquiry.customerEmail ? (
                        <a
                          href={`mailto:${enquiry.customerEmail}`}
                          className="inline-flex items-center gap-1.5 hover:text-ink"
                        >
                          <Mail className="size-3.5" aria-hidden="true" />
                          {enquiry.customerEmail}
                        </a>
                      ) : null}
                      {enquiry.customerPhone ? (
                        <a
                          href={`tel:${enquiry.customerPhone}`}
                          className="inline-flex items-center gap-1.5 hover:text-ink"
                        >
                          <Phone className="size-3.5" aria-hidden="true" />
                          {enquiry.customerPhone}
                        </a>
                      ) : null}
                    </div>
                  </div>

                  <div className="flex shrink-0 flex-wrap items-center gap-2">
                    {nextStatus[enquiry.status] ? (
                      <Button
                        size="sm"
                        onClick={() =>
                          changeStatus(enquiry, nextStatus[enquiry.status])
                        }
                        disabled={isUpdating}
                      >
                        {nextStatus[enquiry.status] === 'CLOSED' ? (
                          <CheckCircle2 className="size-3.5" aria-hidden="true" />
                        ) : null}
                        {actionLabel(enquiry.status)}
                      </Button>
                    ) : null}
                    {enquiry.status !== 'CLOSED' &&
                    enquiry.status !== 'RESPONDED' ? (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => changeStatus(enquiry, 'CLOSED')}
                        disabled={isUpdating}
                      >
                        Close
                      </Button>
                    ) : null}
                  </div>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
