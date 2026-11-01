import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../../auth/authSlice.js'
import { hasPlatformRole } from '../../auth/lib/roleResolver.js'

export function RequirePlatform() {
  const user = useAppSelector(selectCurrentUser)

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (hasPlatformRole(user)) {
    return <Outlet />
  }

  return <Navigate to="/onboarding" replace />
}