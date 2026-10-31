import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { hasPlatformRole, hasOrganizationRole } from '../../auth/lib/roleResolver.js'

export function RequirePlatformMembership() {
  const user = useAppSelector(selectCurrentUser)

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (hasPlatformRole(user)) {
    return <Navigate to="/platform" replace />
  }

  if (hasOrganizationRole(user)) {
    return <Navigate to="/app" replace />
  }

  return <Outlet />
}