import { useState } from 'react'
import { AlertCircle, LoaderCircle } from 'lucide-react'
import { Button } from '../../../components/ui/Button.jsx'
import { Modal } from '../../../components/ui/Modal.jsx'
import {
  useCreateListingMutation,
  useUpdateListingMutation,
} from '../../listings/api/listingApi.js'

const inputClassName =
  'h-11 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink transition-colors placeholder:text-muted/70 hover:border-brand/30 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/15 disabled:cursor-not-allowed disabled:bg-canvas disabled:text-muted'

const textareaClassName = `${inputClassName} h-auto min-h-24 py-2.5`

const accommodationTypes = [
  { value: 'HOSTEL', label: 'Hostel' },
  { value: 'PG', label: 'Paying guest (PG)' },
  { value: 'APARTMENT', label: 'Apartment' },
  { value: 'ROOM', label: 'Room' },
  { value: 'OTHER', label: 'Other' },
]

const pricePeriods = [
  { value: 'DAY', label: 'per day' },
  { value: 'WEEK', label: 'per week' },
  { value: 'MONTH', label: 'per month' },
  { value: 'YEAR', label: 'per year' },
]

function blankToNull(value) {
  if (typeof value !== 'string') {
    return value ?? null
  }

  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

function numberOrNull(value) {
  const trimmed = typeof value === 'string' ? value.trim() : value
  if (trimmed === '' || trimmed === null || trimmed === undefined) {
    return null
  }
  const parsed = Number(trimmed)
  return Number.isFinite(parsed) ? parsed : null
}

export function ListingFormModal({
  open,
  organizationId,
  listing,
  properties = [],
  onClose,
}) {
  const isEditing = Boolean(listing)
  const [createListing, { isLoading: isCreating }] = useCreateListingMutation()
  const [updateListing, { isLoading: isUpdating }] = useUpdateListingMutation()
  const [error, setError] = useState(null)
  const isLoading = isCreating || isUpdating

  const handleSubmit = async (event) => {
    event.preventDefault()
    const values = Object.fromEntries(new FormData(event.target).entries())
    setError(null)

    const body = {
      title: values.title.trim(),
      description: blankToNull(values.description),
      accommodationType: values.accommodationType || null,
      propertyId: numberOrNull(values.propertyId),
      addressLine1: blankToNull(values.addressLine1),
      city: blankToNull(values.city),
      state: blankToNull(values.state),
      postalCode: blankToNull(values.postalCode),
      country: blankToNull(values.country),
      latitude: numberOrNull(values.latitude),
      longitude: numberOrNull(values.longitude),
      basePrice: numberOrNull(values.basePrice),
      currency: blankToNull(values.currency)?.toUpperCase() ?? null,
      pricePeriod: values.pricePeriod || null,
      contactName: blankToNull(values.contactName),
      contactEmail: blankToNull(values.contactEmail),
      contactPhone: blankToNull(values.contactPhone),
    }

    if (body.title.length < 3) {
      setError('Listing title must be at least 3 characters.')
      return
    }

    try {
      if (isEditing) {
        await updateListing({
          organizationId,
          listingId: listing.id,
          ...body,
          version: listing.version,
        }).unwrap()
      } else {
        await createListing({ organizationId, ...body }).unwrap()
      }
      onClose()
    } catch (requestError) {
      setError(
        requestError?.data?.message ||
          'Could not save the listing. Check the details and try again.',
      )
    }
  }

  return (
    <Modal
      open={open}
      title={isEditing ? 'Edit listing' : 'Create listing'}
      description={
        isEditing
          ? 'Update the listing details. Published listings stay live until you archive them.'
          : 'Start a draft now. You can publish it once the essentials are complete.'
      }
      onClose={onClose}
    >
      <form className="space-y-5" onSubmit={handleSubmit}>
        {error ? (
          <div
            role="alert"
            className="flex items-start gap-2.5 rounded-xl border border-danger/15 bg-danger-soft/60 p-3"
          >
            <AlertCircle className="mt-0.5 size-4 shrink-0 text-danger" aria-hidden="true" />
            <p className="text-sm text-ink">{error}</p>
          </div>
        ) : null}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <label className="block sm:col-span-2">
            <span className="text-sm font-medium text-ink">Title</span>
            <input
              name="title"
              required
              minLength={3}
              maxLength={160}
              defaultValue={listing?.title || ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="Sunrise Girls PG near North Campus"
            />
          </label>

          <label className="block sm:col-span-2">
            <span className="text-sm font-medium text-ink">Description</span>
            <textarea
              name="description"
              maxLength={4000}
              defaultValue={listing?.description || ''}
              className={`mt-2 ${textareaClassName}`}
              placeholder="Describe rooms, amenities, meals, house rules, and the neighbourhood."
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Accommodation type</span>
            <select
              name="accommodationType"
              defaultValue={listing?.accommodationType || 'HOSTEL'}
              className={`mt-2 ${inputClassName}`}
            >
              {accommodationTypes.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Property</span>
            <select
              name="propertyId"
              defaultValue={listing?.propertyId ?? ''}
              className={`mt-2 ${inputClassName}`}
            >
              <option value="">No property association</option>
              {properties.map((property) => (
                <option key={property.id} value={property.id}>
                  {property.name} ({property.code})
                </option>
              ))}
            </select>
          </label>

          <label className="block sm:col-span-2">
            <span className="text-sm font-medium text-ink">Address line</span>
            <input
              name="addressLine1"
              maxLength={200}
              defaultValue={listing?.addressLine1 || ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="Plot 42, North Campus Road"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">City</span>
            <input
              name="city"
              maxLength={100}
              defaultValue={listing?.city || ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="Bengaluru"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">State</span>
            <input
              name="state"
              maxLength={100}
              defaultValue={listing?.state || ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="Karnataka"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Postal code</span>
            <input
              name="postalCode"
              maxLength={20}
              defaultValue={listing?.postalCode || ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="560001"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Country</span>
            <input
              name="country"
              maxLength={100}
              defaultValue={listing?.country || 'IN'}
              className={`mt-2 ${inputClassName}`}
              placeholder="IN"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Latitude</span>
            <input
              name="latitude"
              type="number"
              step="any"
              min="-90"
              max="90"
              defaultValue={listing?.latitude ?? ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="12.9716"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Longitude</span>
            <input
              name="longitude"
              type="number"
              step="any"
              min="-180"
              max="180"
              defaultValue={listing?.longitude ?? ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="77.5946"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Base price</span>
            <input
              name="basePrice"
              type="number"
              step="0.01"
              min="0"
              defaultValue={listing?.basePrice ?? ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="8500"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Currency</span>
            <input
              name="currency"
              maxLength={3}
              minLength={3}
              defaultValue={listing?.currency || 'INR'}
              className={`mt-2 ${inputClassName}`}
              placeholder="INR"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Price period</span>
            <select
              name="pricePeriod"
              defaultValue={listing?.pricePeriod || 'MONTH'}
              className={`mt-2 ${inputClassName}`}
            >
              {pricePeriods.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Contact name</span>
            <input
              name="contactName"
              maxLength={120}
              defaultValue={listing?.contactName || ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="Priya Sharma"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Contact email</span>
            <input
              name="contactEmail"
              type="email"
              maxLength={254}
              defaultValue={listing?.contactEmail || ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="owner@example.com"
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-ink">Contact phone</span>
            <input
              name="contactPhone"
              maxLength={40}
              defaultValue={listing?.contactPhone || ''}
              className={`mt-2 ${inputClassName}`}
              placeholder="+91 98765 43210"
            />
          </label>
        </div>

        <p className="text-xs leading-5 text-muted">
          To publish, a listing needs a description, city, latitude, longitude,
          and base price. Drafts can be saved at any time.
        </p>

        <div className="flex items-center justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={isLoading}>
            {isLoading ? (
              <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
            ) : null}
            {isEditing ? 'Save changes' : 'Create draft'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
