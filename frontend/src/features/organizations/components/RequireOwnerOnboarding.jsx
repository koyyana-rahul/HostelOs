import { Navigate, Outlet } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { PageLoader } from '../../../components/feedback/PageLoader.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import {
  hasOrganizationRole,
  hasPlatformRole,
} from '../../auth/lib/roleResolver.js'
import { useListMyOrganizationsQuery } from '../api/organizationApi.js'

export function RequireOwnerOnboarding() {
  const user = useAppSelector(selectCurrentUser)
  const ownerAccount = hasOrganizationRole(user)

  const { data, isLoading, isError, error, refetch, isFetching } =
    useListMyOrganizationsQuery(undefined, {
      skip: !user || !ownerAccount,
    })

  const organizations = Array.isArray(data) ? data : []

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (hasPlatformRole(user)) {
    return <Navigate to="/platform" replace />
  }

  if (!ownerAccount) {
    return <Navigate to="/discover" replace />
  }

  if (isLoading) {
    return <PageLoader label="Loading your organization" />
  }

  if (isError) {
    if (error?.status === 401) {
      return <Navigate to="/login" replace />
    }

    return (
      <ErrorState
        title="Could not load your organizations"
        description={
          error?.data?.message ||
          'We could not retrieve your organization information. Please try again.'
        }
        action={
          <Button onClick={() => refetch()} disabled={isFetching}>
            Try again
          </Button>
        }
      />
    )
  }

  if (organizations.length > 0) {
    return <Navigate to="/app/overview" replace />
  }

  return <Outlet />
}
