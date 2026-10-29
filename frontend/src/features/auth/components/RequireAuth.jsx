import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { PageLoader } from '../../../components/feedback/PageLoader.jsx'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { selectAuthStatus } from '../authSlice.js'

export function RequireAuth() {
  const status = useAppSelector(selectAuthStatus)
  const location = useLocation()

  if (status === 'checking') {
    return <PageLoader label="Checking your session" />
  }

  if (status !== 'authenticated') {
    return (
      <Navigate
        to="/login"
        state={{ from: location }}
        replace
      />
    )
  }

  return <Outlet />
}
