import { Navigate, Outlet, useParams } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { useGetMembershipsQuery } from '../../auth/api/authApi.js'
import { useAppDispatch } from '../../../hooks/useAppDispatch.js'
import { selectCurrentOrganizationId, setCurrentOrganization } from '../organizationSlice.js'
import { useEffect } from 'react'

export function RequireOrgAccess() {
  const user = useAppSelector(selectCurrentUser)
  const { organizationId } = useParams()
  const dispatch = useAppDispatch()
  const currentOrgId = useAppSelector(selectCurrentOrganizationId)
  const { data: memberships = [], isLoading } = useGetMembershipsQuery()

  useEffect(() => {
    if (organizationId) {
      const id = Number(organizationId)
      if (!Number.isNaN(id)) {
        dispatch(setCurrentOrganization(id))
      }
    }
  }, [organizationId, dispatch])

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <LoaderCircle className="size-5 animate-spin text-muted" aria-hidden="true" />
      </div>
    )
  }

  const list = Array.isArray(memberships) ? memberships : []
  const activeMemberships = list.filter((m) => m && m.status === 'ACTIVE')
  if (activeMemberships.length === 0) {
    return <Navigate to="/onboarding" replace />
  }

  const targetId = organizationId ? Number(organizationId) : currentOrgId
  const membership = activeMemberships.find((m) => m.organizationId === targetId) || activeMemberships[0]

  if (!membership || membership.status !== 'ACTIVE') {
    return <Navigate to="/onboarding" replace />
  }

  return <Outlet context={{ membership }} />
}
