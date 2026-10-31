import { Navigate } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { useListMyOrganizationsQuery } from '../../organizations/api/organizationApi.js'
import {
  getLandingPath,
  hasOrganizationRole,
} from '../../auth/lib/roleResolver.js'

export default function HomeRedirectPage() {
  const user = useAppSelector(selectCurrentUser)
  const ownerAccount = hasOrganizationRole(user)
  const { data = [], isLoading } = useListMyOrganizationsQuery(undefined, {
    skip: !user || !ownerAccount,
  })

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (ownerAccount && isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <LoaderCircle className="size-5 animate-spin text-muted" aria-hidden="true" />
      </div>
    )
  }

  return <Navigate to={getLandingPath(user, data)} replace />
}
