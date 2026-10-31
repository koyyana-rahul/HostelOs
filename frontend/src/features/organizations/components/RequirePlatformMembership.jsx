import { Navigate, Outlet } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { useGetMembershipsQuery } from '../../auth/api/authApi.js'
import { hasPlatformRole } from '../../auth/lib/roleResolver.js'

export function RequirePlatformMembership() {
  const user = useAppSelector(selectCurrentUser)
  const { data: memberships = [], isLoading } = useGetMembershipsQuery()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (hasPlatformRole(user)) {
    return <Navigate to="/platform" replace />
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
  if (activeMemberships.length > 0) {
    return <Navigate to="/app" replace />
  }

  return <Outlet />
}
