import { AlertCircle, CheckCircle2, LoaderCircle, MapPin } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Badge } from '../../../components/ui/Badge.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { Skeleton } from '../../../components/ui/Skeleton.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { hasCustomerRole } from '../../auth/lib/roleResolver.js'
import { useCreateEnquiryMutation } from '../../enquiries/api/enquiryApi.js'
import { useGetPublicListingQuery } from '../../listings/api/listingApi.js'
import { formatMoney, titleCase } from '../../../utils/format.js'
import { ListingImageGallery } from '../components/ListingImageGallery.jsx'

const inputClassName =
  'h-11 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink transition-colors placeholder:text-muted/70 hover:border-brand/30 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15 disabled:cursor-not-allowed disabled:bg-canvas disabled:text-muted'

function buildAddress(listing) {
  if (!listing) {
    return ''
  }

  return [
    listing.addressLine1,
    listing.city,
    listing.state,
    listing.postalCode,
    listing.country,
  ]
    .filter(Boolean)
    .join(', ')
}

function DetailSkeleton() {
  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <Skeleton variant="text" className="h-4 w-24" />
      <Skeleton variant="text" className="mt-4 h-10 w-2/3" />
      <Skeleton variant="text" className="mt-3 h-5 w-1/2" />
      <Card className="mt-8 space-y-3">
        <Skeleton variant="text" className="h-4 w-full" />
        <Skeleton variant="text" className="h-4 w-full" />
        <Skeleton variant="text" className="h-4 w-3/4" />
      </Card>
    </div>
  )
}

export default function ListingDetailsPage() {
  const { listingId } = useParams()
  const currentUser = useAppSelector(selectCurrentUser)
  const canEnquire = hasCustomerRole(currentUser)
  const [createEnquiry, { isLoading: isSubmitting }] = useCreateEnquiryMutation()
  const {
    data: listing,
    isLoading,
    isError,
    error,
  } = useGetPublicListingQuery(listingId, { skip: !listingId })
  const [message, setMessage] = useState('')
  const [formError, setFormError] = useState('')
  const [success, setSuccess] = useState(false)
  const [customerName, setCustomerName] = useState(currentUser?.displayName ?? '')
  const [customerEmail, setCustomerEmail] = useState(
    currentUser?.loginIdentifier ?? '',
  )
  const [customerPhone, setCustomerPhone] = useState('')

  useDocumentTitle(listing?.title ?? 'Listing details')

  const address = buildAddress(listing)

  const handleSubmit = async (event) => {
    event.preventDefault()

    if (!message.trim()) {
      setFormError('Tell the owner what you are looking for.')
      return
    }

    setFormError('')
    setSuccess(false)

    try {
      await createEnquiry({
        listingId: listing.id,
        customerName: customerName.trim(),
        customerEmail: customerEmail.trim(),
        customerPhone: customerPhone.trim(),
        message: message.trim(),
      }).unwrap()
      setMessage('')
      setSuccess(true)
    } catch (requestError) {
      setFormError(
        requestError?.data?.message ??
          'Could not send your enquiry. Please try again.',
      )
    }
  }

  if (isLoading) {
    return <DetailSkeleton />
  }

  if (isError) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
        <p className="text-sm font-medium text-danger">
          {error?.data?.message ?? 'Could not load this listing.'}
        </p>
        <Button as={Link} to="/discover" className="mt-4">
          Back to discover
        </Button>
      </div>
    )
  }

  if (!listing) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
        <p className="text-sm font-medium text-danger">Listing not found.</p>
        <Button as={Link} to="/discover" className="mt-4">
          Back to discover
        </Button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <Link to="/discover" className="text-sm text-brand hover:underline">
        Back to discover
      </Link>

      <div className="mt-4 flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">
            {listing.organizationName || 'HostelOS listing'}
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">
            {listing.title}
          </h1>
          {address ? (
            <p className="mt-3 flex items-center gap-1.5 text-sm text-muted">
              <MapPin className="size-4" aria-hidden="true" />
              {address}
            </p>
          ) : null}
        </div>
        <div className="text-right">
          <p className="text-2xl font-semibold text-ink">
            {formatMoney(listing.basePrice, listing.currency)}
          </p>
          <p className="text-xs text-muted">
            {listing.pricePeriod
              ? `per ${titleCase(listing.pricePeriod)}`
              : 'Contact the owner for pricing'}
          </p>
        </div>
      </div>

      <div className="mt-6 flex flex-wrap gap-2">
        <Badge tone="brand">
          {titleCase(listing.accommodationType) || 'Accommodation'}
        </Badge>
        {listing.city ? <Badge>{listing.city}</Badge> : null}
      </div>

      <Card className="mt-6">
        <ListingImageGallery listing={listing} />
      </Card>

      <Card className="mt-8">
        <h2 className="text-sm font-semibold uppercase tracking-[0.12em] text-brand">
          About this stay
        </h2>
        <p className="mt-3 whitespace-pre-line text-sm leading-6 text-muted">
          {listing.description || 'The owner has not added a description yet.'}
        </p>
      </Card>

      <Card className="mt-6">
        <h2 className="text-sm font-semibold uppercase tracking-[0.12em] text-brand">
          Contact
        </h2>
        <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-muted">Contact name</dt>
            <dd className="mt-1 text-ink">
              {listing.contactName || listing.organizationName || 'Not provided'}
            </dd>
          </div>
          <div>
            <dt className="text-muted">Email</dt>
            <dd className="mt-1 text-ink">
              {listing.contactEmail ? (
                <a
                  href={`mailto:${listing.contactEmail}`}
                  className="text-brand hover:underline"
                >
                  {listing.contactEmail}
                </a>
              ) : (
                'Not provided'
              )}
            </dd>
          </div>
          <div>
            <dt className="text-muted">Phone</dt>
            <dd className="mt-1 text-ink">
              {listing.contactPhone ? (
                <a
                  href={`tel:${listing.contactPhone}`}
                  className="text-brand hover:underline"
                >
                  {listing.contactPhone}
                </a>
              ) : (
                'Not provided'
              )}
            </dd>
          </div>
        </dl>
      </Card>

      <Card className="mt-6">
        <h2 className="text-sm font-semibold uppercase tracking-[0.12em] text-brand">
          Send an enquiry
        </h2>

        {!currentUser ? (
          <div className="mt-4">
            <p className="text-sm text-muted">
              Sign in with a customer account to contact this owner.
            </p>
            <Button
              as={Link}
              to="/login"
              state={{ from: `/discover/${listing.id}` }}
              className="mt-4"
            >
              Sign in to enquire
            </Button>
          </div>
        ) : !canEnquire ? (
          <p className="mt-4 text-sm text-muted">
            Only customer accounts can send enquiries.
          </p>
        ) : (
          <form className="mt-4 space-y-4" onSubmit={handleSubmit}>
            {success ? (
              <div
                role="status"
                className="flex items-start gap-2 rounded-xl border border-success/15 bg-success-soft/60 p-3 text-sm text-ink"
              >
                <CheckCircle2
                  className="mt-0.5 size-4 shrink-0 text-success"
                  aria-hidden="true"
                />
                Your enquiry was sent to the owner.
              </div>
            ) : null}

            {formError ? (
              <div
                role="alert"
                className="flex items-start gap-2 rounded-xl border border-danger/15 bg-danger-soft/60 p-3 text-sm text-ink"
              >
                <AlertCircle
                  className="mt-0.5 size-4 shrink-0 text-danger"
                  aria-hidden="true"
                />
                {formError}
              </div>
            ) : null}

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="enquiry-name" className="text-sm font-medium text-ink">
                  Your name
                </label>
                <input
                  id="enquiry-name"
                  value={customerName}
                  onChange={(event) => setCustomerName(event.target.value)}
                  maxLength={120}
                  required
                  disabled={isSubmitting}
                  className={`mt-2 ${inputClassName}`}
                />
              </div>
              <div>
                <label htmlFor="enquiry-email" className="text-sm font-medium text-ink">
                  Email
                </label>
                <input
                  id="enquiry-email"
                  type="email"
                  value={customerEmail}
                  onChange={(event) => setCustomerEmail(event.target.value)}
                  maxLength={254}
                  required
                  disabled={isSubmitting}
                  className={`mt-2 ${inputClassName}`}
                />
              </div>
              <div>
                <label htmlFor="enquiry-phone" className="text-sm font-medium text-ink">
                  Phone
                </label>
                <input
                  id="enquiry-phone"
                  value={customerPhone}
                  onChange={(event) => setCustomerPhone(event.target.value)}
                  maxLength={30}
                  required
                  disabled={isSubmitting}
                  className={`mt-2 ${inputClassName}`}
                />
              </div>
            </div>

            <div>
              <label htmlFor="enquiry-message" className="text-sm font-medium text-ink">
                Message
              </label>
              <textarea
                id="enquiry-message"
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                rows={4}
                maxLength={2000}
                required
                disabled={isSubmitting}
                placeholder="Tell the owner about your move-in dates, budget, and preferences."
                className={`mt-2 ${inputClassName} h-auto py-3`}
              />
              <p className="mt-1.5 text-xs text-muted">{message.length}/2000</p>
            </div>

            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? (
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
              ) : null}
              {isSubmitting ? 'Sending...' : 'Send enquiry'}
            </Button>
          </form>
        )}
      </Card>
    </div>
  )
}
