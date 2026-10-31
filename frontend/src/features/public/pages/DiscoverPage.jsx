import { Link, useSearchParams } from 'react-router-dom'
import { Building2, LoaderCircle, MapPin, Search } from 'lucide-react'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { formatMoney, titleCase } from '../../../utils/format.js'
import { useSearchPublicListingsQuery } from '../../listings/api/listingApi.js'

const inputClassName =
  'h-11 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink transition-colors placeholder:text-muted/70 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15'

const accommodationTypes = [
  { value: '', label: 'All types' },
  { value: 'HOSTEL', label: 'Hostel' },
  { value: 'PG', label: 'PG' },
  { value: 'APARTMENT', label: 'Apartment' },
  { value: 'ROOM', label: 'Room' },
  { value: 'OTHER', label: 'Other' },
]

function ListingCard({ listing }) {
  return (
    <Link
      to={`/discover/${listing.id}`}
      className="group block focus:outline-none"
    >
      <Card className="h-full overflow-hidden transition-colors group-hover:border-brand/30">
        <div className="flex h-28 items-end bg-gradient-to-br from-brand-soft via-canvas to-surface p-4">
          <span className="grid size-10 place-items-center rounded-xl bg-surface/80 text-brand shadow-sm">
            <Building2 className="size-5" aria-hidden="true" />
          </span>
        </div>
        <div className="p-4">
          <div className="flex items-start justify-between gap-2">
            <h2 className="line-clamp-2 text-sm font-semibold text-ink">
              {listing.title}
            </h2>
            <span className="shrink-0 rounded-full bg-brand-soft px-2 py-0.5 text-[10px] font-medium text-brand-strong">
              {titleCase(listing.accommodationType)}
            </span>
          </div>
          <p className="mt-1 flex items-center gap-1 text-xs text-muted">
            <MapPin className="size-3" aria-hidden="true" />
            {[listing.city, listing.state].filter(Boolean).join(', ') ||
              'Location unavailable'}
          </p>
          <p className="mt-3 text-sm font-semibold text-brand">
            {formatMoney(listing.basePrice, listing.currency)}
            {listing.pricePeriod ? (
              <span className="font-normal text-muted">
                {' '}
                / {listing.pricePeriod.toLowerCase()}
              </span>
            ) : null}
          </p>
        </div>
      </Card>
    </Link>
  )
}

export default function DiscoverPage() {
  useDocumentTitle('Discover hostels and PGs')
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') || ''
  const city = searchParams.get('city') || ''
  const type = searchParams.get('type') || ''
  const minPrice = searchParams.get('minPrice') || ''
  const maxPrice = searchParams.get('maxPrice') || ''
  const page = Number(searchParams.get('page')) || 0

  const {
    data,
    isLoading,
    isFetching,
    isError,
    error,
    refetch,
  } = useSearchPublicListingsQuery({
    q,
    city,
    type: type || undefined,
    minPrice: minPrice || undefined,
    maxPrice: maxPrice || undefined,
    page,
    size: 12,
  })

  const setFilter = (name, value) => {
    setSearchParams((previous) => {
      const next = new URLSearchParams(previous.toString())
      if (value) {
        next.set(name, value)
      } else {
        next.delete(name)
      }
      next.delete('page')
      return next
    })
  }

  const listings = data?.content || []
  const totalPages = data?.totalPages || 0
  const totalElements = data?.totalElements || 0
  const hasFilters = Boolean(q || city || type || minPrice || maxPrice)

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <header className="max-w-2xl">
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">
          Discover
        </p>
        <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">
          Find hostels and PGs
        </h1>
        <p className="mt-3 text-lg text-muted">
          Browse published accommodations from HostelOS owners. Send an enquiry
          directly from any listing.
        </p>
      </header>

      <Card className="mt-8 p-4 sm:p-5">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <label className="block lg:col-span-2">
            <span className="text-xs font-medium text-muted">Search</span>
            <div className="relative mt-1.5">
              <Search
                className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted"
                aria-hidden="true"
              />
              <input
                type="search"
                value={q}
                onChange={(event) => setFilter('q', event.target.value)}
                placeholder="Title or neighbourhood"
                className={`${inputClassName} pl-9`}
              />
            </div>
          </label>

          <label className="block">
            <span className="text-xs font-medium text-muted">City</span>
            <input
              value={city}
              onChange={(event) => setFilter('city', event.target.value)}
              placeholder="Bengaluru"
              className={`mt-1.5 ${inputClassName}`}
            />
          </label>

          <label className="block">
            <span className="text-xs font-medium text-muted">Type</span>
            <select
              value={type}
              onChange={(event) => setFilter('type', event.target.value)}
              className={`mt-1.5 ${inputClassName}`}
            >
              {accommodationTypes.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <div className="grid grid-cols-2 gap-2">
            <label className="block">
              <span className="text-xs font-medium text-muted">Min price</span>
              <input
                type="number"
                min="0"
                value={minPrice}
                onChange={(event) => setFilter('minPrice', event.target.value)}
                placeholder="0"
                className={`mt-1.5 ${inputClassName}`}
              />
            </label>
            <label className="block">
              <span className="text-xs font-medium text-muted">Max price</span>
              <input
                type="number"
                min="0"
                value={maxPrice}
                onChange={(event) => setFilter('maxPrice', event.target.value)}
                placeholder="Any"
                className={`mt-1.5 ${inputClassName}`}
              />
            </label>
          </div>
        </div>

        {hasFilters ? (
          <div className="mt-3 flex justify-end">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setSearchParams(new URLSearchParams())}
            >
              Clear filters
            </Button>
          </div>
        ) : null}
      </Card>

      <div className="mt-8 flex items-center justify-between gap-3">
        <h2 className="text-sm font-semibold text-ink">
          {isLoading
            ? 'Loading listings'
            : `${totalElements} ${totalElements === 1 ? 'listing' : 'listings'}`}
        </h2>
        {isFetching && !isLoading ? (
          <LoaderCircle className="size-4 animate-spin text-muted" aria-hidden="true" />
        ) : null}
      </div>

      {isError ? (
        <div className="mt-6">
          <ErrorState
            title="Could not load listings"
            description={
              error?.data?.message ||
              'The listing service is unavailable right now. Try again.'
            }
            action={
              <Button onClick={() => refetch()} variant="secondary">
                Retry
              </Button>
            }
          />
        </div>
      ) : isLoading ? (
        <div className="mt-6 flex h-48 items-center justify-center">
          <LoaderCircle className="size-5 animate-spin text-muted" aria-hidden="true" />
        </div>
      ) : listings.length === 0 ? (
        <Card className="mt-6 p-8 text-center">
          <p className="font-medium text-ink">No listings found</p>
          <p className="mt-1 text-sm text-muted">
            {hasFilters
              ? 'Try widening your search or clearing the filters.'
              : 'Published listings will appear here as owners go live.'}
          </p>
          {hasFilters ? (
            <div className="mt-4">
              <Button
                variant="secondary"
                onClick={() => setSearchParams(new URLSearchParams())}
              >
                Clear filters
              </Button>
            </div>
          ) : null}
        </Card>
      ) : (
        <>
          <ul className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {listings.map((listing) => (
              <li key={listing.id}>
                <ListingCard listing={listing} />
              </li>
            ))}
          </ul>

          {totalPages > 1 ? (
            <nav
              aria-label="Pagination"
              className="mt-8 flex items-center justify-between gap-3"
            >
              <Button
                variant="secondary"
                size="sm"
                disabled={page <= 0}
                onClick={() =>
                  setSearchParams((previous) => {
                    const next = new URLSearchParams(previous.toString())
                    next.set('page', String(Math.max(0, page - 1)))
                    return next
                  })
                }
              >
                Previous
              </Button>
              <p className="text-xs text-muted">
                Page {page + 1} of {totalPages}
              </p>
              <Button
                variant="secondary"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() =>
                  setSearchParams((previous) => {
                    const next = new URLSearchParams(previous.toString())
                    next.set('page', String(page + 1))
                    return next
                  })
                }
              >
                Next
              </Button>
            </nav>
          ) : null}
        </>
      )}
    </div>
  )
}
