import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { hasOrganizationRole } from '../../auth/lib/roleResolver.js'

export function RequireOrgAccess() {
  const user = useAppSelector(selectCurrentUser)

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (hasOrganizationRole(user)) {
    return <Outlet />
  }

  return <Navigate to="/onboarding" replace />
}