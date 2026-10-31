import { Navigate, Outlet } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { useGetMembershipsQuery } from '../../auth/api/authApi.js'
import { hasPlatformRole } from '../../auth/lib/roleResolver.js'

export function RequirePlatform() {
  const user = useAppSelector(selectCurrentUser)
  const { data: memberships = [], isLoading } = useGetMembershipsQuery(undefined, {
    skip: !user || hasPlatformRole(user),
  })

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (hasPlatformRole(user)) {
    return <Outlet />
  }

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <LoaderCircle className="size-5 animate-spin text-muted" aria-hidden="true" />
      </div>
    )
  }

  const list = Array.isArray(memberships) ? memberships : []
  if (list.some((m) => m && m.status === 'ACTIVE')) {
    return <Navigate to="/app" replace />
  }

  return <Navigate to="/onboarding" replace />
}
