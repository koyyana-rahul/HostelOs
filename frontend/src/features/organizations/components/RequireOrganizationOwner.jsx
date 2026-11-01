import { useEffect, useMemo } from 'react'
import { Navigate, Outlet } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { PageLoader } from '../../../components/feedback/PageLoader.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { useAppDispatch } from '../../../hooks/useAppDispatch.js'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import {
  hasOrganizationRole,
  hasPlatformRole,
} from '../../auth/lib/roleResolver.js'
import { useListMyOrganizationsQuery } from '../api/organizationApi.js'
import {
  selectCurrentOrganizationId,
  setCurrentOrganization,
} from '../organizationSlice.js'

export function RequireOrganizationOwner() {
  const dispatch = useAppDispatch()
  const user = useAppSelector(selectCurrentUser)
  const currentOrganizationId = useAppSelector(selectCurrentOrganizationId)
  const ownerAccount = hasOrganizationRole(user)

  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useListMyOrganizationsQuery(undefined, {
    skip: !user || !ownerAccount,
  })

  const organizations = useMemo(
    () => (Array.isArray(data) ? data : []),
    [data],
  )

  useEffect(() => {
    if (organizations.length === 0) {
      return
    }

    const currentExists = organizations.some(
      (organization) => organization.id === currentOrganizationId,
    )

    if (!currentExists) {
      dispatch(setCurrentOrganization(organizations[0].id))
    }
  }, [organizations, currentOrganizationId, dispatch])

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
    return <PageLoader label="Loading your workspace" />
  }

  if (isError) {
    if (error?.status === 401) {
      return <Navigate to="/login" replace />
    }

    return (
      <ErrorState
        title="Could not load your workspace"
        description={
          error?.data?.message ||
          'We could not retrieve your organization. Try again in a moment.'
        }
        action={
          <Button onClick={() => refetch()} disabled={isFetching}>
            Try again
          </Button>
        }
      />
    )
  }

  if (organizations.length === 0) {
    return <Navigate to="/onboarding/create" replace />
  }

  return <Outlet />
}
