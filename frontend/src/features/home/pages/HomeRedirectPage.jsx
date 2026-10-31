import { Navigate } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { useGetMembershipsQuery } from '../../auth/api/authApi.js'
import { getLandingPath } from '../../auth/lib/roleResolver.js'

export default function HomeRedirectPage() {
  const user = useAppSelector(selectCurrentUser)
  const { data: memberships = [], isLoading } = useGetMembershipsQuery(undefined, {
    skip: !user,
  })

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

  return <Navigate to={getLandingPath(user, memberships)} replace />
}
