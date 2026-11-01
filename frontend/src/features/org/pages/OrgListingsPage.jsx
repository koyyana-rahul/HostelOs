import { useState } from 'react'
import {
  AlertCircle,
  Archive,
  Eye,
  EyeOff,
  LoaderCircle,
  Pencil,
  Plus,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { Badge } from '../../../components/ui/Badge.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { formatMoney, titleCase } from '../../../utils/format.js'
import {
  useChangeListingStatusMutation,
  useListListingsQuery,
} from '../../listings/api/listingApi.js'
import { useListPropertiesQuery } from '../../organizations/api/propertyApi.js'
import { useOwnerOrganization } from '../../organizations/hooks/useOwnerOrganization.js'
import { ListingFormModal } from '../components/ListingFormModal.jsx'

const statusFilters = [
  { value: '', label: 'All listings' },
  { value: 'DRAFT', label: 'Drafts' },
  { value: 'PUBLISHED', label: 'Published' },
  { value: 'ARCHIVED', label: 'Archived' },
]

function statusTone(status) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'DRAFT') return 'warning'
  return 'neutral'
}

export default function OrgListingsPage() {
  useDocumentTitle('Listings')
  const { organizationId } = useOwnerOrganization()
  const [statusFilter, setStatusFilter] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [actionError, setActionError] = useState(null)
  const [changeStatus, { isLoading: isChangingStatus }] =
    useChangeListingStatusMutation()

  const {
    data: listings = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useListListingsQuery(
    { organizationId, status: statusFilter || undefined },
    { skip: !organizationId },
  )

  const { data: properties = [] } = useListPropertiesQuery(
    { organizationId },
    { skip: !organizationId },
  )

  const updateListingStatus = async (listing, status) => {
    setActionError(null)
    try {
      await changeStatus({
        organizationId,
        listingId: listing.id,
        status,
        version: listing.version,
      }).unwrap()
    } catch (requestError) {
      setActionError(
        requestError?.data?.message ||
          'Could not update the listing. Refresh the page and try again.',
      )
    }
  }

  if (isError) {
    return (
      <ErrorState
        title="Could not load listings"
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
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">
            Listings
          </h1>
          <p className="mt-1 text-sm text-muted">
            Draft, publish, and archive the accommodation customers discover.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" aria-hidden="true" />
          New listing
        </Button>
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
          {listings.length} {listings.length === 1 ? 'listing' : 'listings'}
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
      ) : listings.length === 0 ? (
        <Card className="p-8 text-center">
          <p className="font-medium text-ink">
            {statusFilter ? 'No listings with this status' : 'No listings yet'}
          </p>
          <p className="mt-1 text-sm text-muted">
            {statusFilter
              ? 'Try another filter or create a new listing.'
              : 'Create a draft, complete the details, then publish it for customers.'}
          </p>
          <div className="mt-4">
            <Button onClick={() => setCreateOpen(true)}>
              <Plus className="size-4" aria-hidden="true" />
              New listing
            </Button>
          </div>
        </Card>
      ) : (
        <ul className="space-y-4">
          {listings.map((listing) => (
            <li key={listing.id}>
              <Card className="p-5">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h2 className="text-base font-semibold text-ink">
                        {listing.title}
                      </h2>
                      <Badge tone={statusTone(listing.status)}>
                        {titleCase(listing.status)}
                      </Badge>
                      <Badge tone="neutral">
                        {titleCase(listing.accommodationType)}
                      </Badge>
                    </div>
                    <p className="mt-1 text-xs text-muted">
                      {[listing.city, listing.state]
                        .filter(Boolean)
                        .join(', ') || 'No address yet'}
                      {listing.propertyName
                        ? ` · ${listing.propertyName}`
                        : ''}
                    </p>
                    <p className="mt-2 text-sm text-muted">
                      {formatMoney(listing.basePrice, listing.currency)}{' '}
                      {listing.pricePeriod
                        ? `/${listing.pricePeriod.toLowerCase()}`
                        : ''}
                    </p>
                    {listing.status === 'DRAFT' ? (
                      <p className="mt-2 text-xs text-warning">
                        Drafts need a description, city, coordinates, and price
                        before they can be published.
                      </p>
                    ) : null}
                  </div>

                  <div className="flex shrink-0 flex-wrap items-center gap-2">
                    {listing.status === 'PUBLISHED' ? (
                      <Button
                        as={Link}
                        to={`/discover/${listing.id}`}
                        size="sm"
                        variant="secondary"
                      >
                        <Eye className="size-3.5" aria-hidden="true" />
                        View public
                      </Button>
                    ) : null}
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => setEditing(listing)}
                    >
                      <Pencil className="size-3.5" aria-hidden="true" />
                      Edit
                    </Button>
                    {listing.status === 'DRAFT' ? (
                      <Button
                        size="sm"
                        onClick={() => updateListingStatus(listing, 'PUBLISHED')}
                        disabled={isChangingStatus}
                      >
                        Publish
                      </Button>
                    ) : null}
                    {listing.status === 'PUBLISHED' ? (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => updateListingStatus(listing, 'DRAFT')}
                        disabled={isChangingStatus}
                      >
                        <EyeOff className="size-3.5" aria-hidden="true" />
                        Unpublish
                      </Button>
                    ) : null}
                    {listing.status !== 'ARCHIVED' ? (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => updateListingStatus(listing, 'ARCHIVED')}
                        disabled={isChangingStatus}
                      >
                        <Archive className="size-3.5" aria-hidden="true" />
                        Archive
                      </Button>
                    ) : null}
                  </div>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <ListingFormModal
        key={`create-${createOpen}`}
        open={createOpen}
        organizationId={organizationId}
        properties={properties}
        onClose={() => setCreateOpen(false)}
      />
      <ListingFormModal
        key={`edit-${editing?.id ?? 'none'}`}
        open={Boolean(editing)}
        organizationId={organizationId}
        listing={editing}
        properties={properties}
        onClose={() => setEditing(null)}
      />
    </div>
  )
}
