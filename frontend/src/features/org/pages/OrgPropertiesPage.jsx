import { useState } from 'react'
import { AlertCircle, LoaderCircle, Pencil, Plus, Trash2 } from 'lucide-react'
import { Badge } from '../../../components/ui/Badge.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { useDocumentTitle } from '../../../hooks/useDocumentTitle.js'
import {
  useChangePropertyStatusMutation,
  useListPropertiesQuery,
} from '../../organizations/api/propertyApi.js'
import { useOwnerOrganization } from '../../organizations/hooks/useOwnerOrganization.js'
import { PropertyFormModal } from '../components/PropertyFormModal.jsx'

function statusTone(status) {
  if (status === 'ACTIVE') return 'success'
  if (status === 'INACTIVE') return 'warning'
  return 'neutral'
}

export default function OrgPropertiesPage() {
  useDocumentTitle('Properties')
  const { organizationId } = useOwnerOrganization()
  const [createOpen, setCreateOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [actionError, setActionError] = useState(null)
  const [changeStatus, { isLoading: isChangingStatus }] =
    useChangePropertyStatusMutation()

  const {
    data: properties = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useListPropertiesQuery(
    { organizationId },
    { skip: !organizationId },
  )

  const archiveProperty = async (property) => {
    setActionError(null)
    try {
      await changeStatus({
        organizationId,
        propertyId: property.id,
        status: 'ARCHIVED',
      }).unwrap()
    } catch (requestError) {
      setActionError(
        requestError?.data?.message ||
          'Could not archive the property. Try again.',
      )
    }
  }

  if (isError) {
    return (
      <ErrorState
        title="Could not load properties"
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
      <header className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">
            Properties
          </h1>
          <p className="mt-1 text-sm text-muted">
            Manage hostel and PG properties for your organization.
          </p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="size-4" aria-hidden="true" />
          Add property
        </Button>
      </header>

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
      ) : properties.length === 0 ? (
        <Card className="p-8 text-center">
          <p className="font-medium text-ink">No properties yet</p>
          <p className="mt-1 text-sm text-muted">
            Create your first property to start managing accommodation.
          </p>
          <div className="mt-4">
            <Button onClick={() => setCreateOpen(true)}>
              <Plus className="size-4" aria-hidden="true" />
              Add property
            </Button>
          </div>
        </Card>
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2">
          {properties.map((property) => (
            <li key={property.id}>
              <Card className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-ink">
                      {property.name}
                    </p>
                    <p className="text-xs text-muted">{property.code}</p>
                  </div>
                  <Badge tone={statusTone(property.status)}>
                    {property.status}
                  </Badge>
                </div>
                <p className="mt-2 truncate text-sm text-muted">
                  {[property.city, property.state].filter(Boolean).join(', ') ||
                    'No address'}
                </p>
                <div className="mt-4 flex items-center gap-2">
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => setEditing(property)}
                  >
                    <Pencil className="size-3.5" aria-hidden="true" />
                    Edit
                  </Button>
                  {property.status !== 'ARCHIVED' ? (
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => archiveProperty(property)}
                      disabled={isChangingStatus}
                    >
                      <Trash2 className="size-3.5" aria-hidden="true" />
                      Archive
                    </Button>
                  ) : null}
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <PropertyFormModal
        key={`create-${createOpen}`}
        open={createOpen}
        organizationId={organizationId}
        onClose={() => setCreateOpen(false)}
      />
      <PropertyFormModal
        key={`edit-${editing?.id ?? 'none'}`}
        open={!!editing}
        organizationId={organizationId}
        property={editing}
        onClose={() => setEditing(null)}
      />
    </div>
  )
}
