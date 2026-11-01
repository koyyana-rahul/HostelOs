import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectCurrentUser } from '../authSlice.js'
import { getLandingPath, hasCustomerRole } from '../lib/roleResolver.js'

export function RequireCustomer() {
  const user = useAppSelector(selectCurrentUser)
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (!hasCustomerRole(user)) {
    return <Navigate to={getLandingPath(user)} replace />
  }

  return <Outlet />
}
