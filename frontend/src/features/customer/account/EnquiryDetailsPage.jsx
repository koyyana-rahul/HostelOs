import { Link, useParams } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import { formatDate, titleCase } from '../../../utils/format.js'
import { useGetEnquiryQuery } from '../api/customerApi.js'

export default function EnquiryDetailsPage() {
  const { enquiryId } = useParams()
  useDocumentTitle('Enquiry details')
  const { data: enquiry, isLoading, isError, error, refetch } =
    useGetEnquiryQuery(enquiryId, { skip: !enquiryId })

  if (isLoading) {
    return <LoaderCircle className="mx-auto size-5 animate-spin text-muted" aria-label="Loading enquiry" />
  }
  if (isError) {
    return (
      <ErrorState
        title="Could not load enquiry"
        description={error?.data?.message ?? 'Try again in a moment.'}
        action={<Button onClick={() => refetch()} variant="secondary">Retry</Button>}
      />
    )
  }

  return (
    <Card className="space-y-4 p-6">
      <div>
        <p className="text-xs uppercase tracking-[0.16em] text-brand">Enquiry</p>
        <h2 className="mt-2 text-xl font-semibold text-ink">{enquiry?.listingTitle}</h2>
        <p className="mt-1 text-sm text-muted">{enquiry?.organizationName}</p>
      </div>
      <p className="text-sm text-muted">
        Status: <span className="font-medium text-ink">{titleCase(enquiry?.status || '')}</span>
      </p>
      <p className="whitespace-pre-line text-sm text-ink">{enquiry?.message}</p>
      <p className="text-xs text-muted">Created {formatDate(enquiry?.createdAt)}</p>
      <Button as={Link} to="/my-enquiries" variant="secondary">Back to enquiries</Button>
    </Card>
  )
}
