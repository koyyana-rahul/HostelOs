import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import { PageLoader } from '../../../components/feedback/PageLoader.jsx'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { useAppDispatch } from '../../../hooks/useAppDispatch.js'
import {
  selectAuthStatus,
  sessionCheckStarted,
  sessionEstablished,
  sessionCheckFailed,
} from '../authSlice.js'
import { useGetCurrentUserQuery } from '../api/authApi.js'

const PUBLIC_ROUTES = new Set([
  '/',
  '/discover',
  '/for-owners',
  '/features',
  '/about',
  '/contact',
  '/login',
  '/register',
])

function isPublicRoute(pathname) {
  return (
    PUBLIC_ROUTES.has(pathname) ||
    pathname.startsWith('/discover/')
  )
}

export function AuthGate() {
  const dispatch = useAppDispatch()
  const status = useAppSelector(selectAuthStatus)
  const location = useLocation()

  const { data, isLoading, isError, error, refetch } = useGetCurrentUserQuery(undefined, {
    skip: status === 'authenticated' || status === 'anonymous',
  })

  useEffect(() => {
    if (status === 'checking') {
      dispatch(sessionCheckStarted())
    }
  }, [status, dispatch])

  useEffect(() => {
    if (status === 'checking' && !isLoading && !isError && data) {
      dispatch(sessionEstablished(data))
    }
  }, [data, isLoading, isError, dispatch, status])

  useEffect(() => {
    if (status === 'checking' && isError) {
      dispatch(sessionCheckFailed())
    }
  }, [isError, dispatch, status])

  if (status === 'checking') {
    if (isLoading) {
      return <PageLoader label="Checking your session" />
    }
    if (isError) {
      if (error?.status === 401) {
        return <Navigate to="/login" state={{ from: location }} replace />
      }
      return (
        <ErrorState
          title="Authentication unavailable"
          description="We could not verify your session. This is usually temporary. Please try again."
          action={
            <Button onClick={() => refetch()} disabled={isLoading}>
              Try again
            </Button>
          }
        />
      )
    }
    if (data) {
      return <Outlet />
    }
    return <PageLoader label="Checking your session" />
  }

  if (status === 'unavailable') {
    return (
      <ErrorState
        title="Authentication unavailable"
        description="We could not verify your session. This is usually temporary. Please try again."
        action={
          <Button
            onClick={() => {
              dispatch(sessionCheckStarted())
              refetch()
            }}
            disabled={isLoading}
          >
            Try again
          </Button>
        }
      />
    )
  }

  if (status === 'authenticated') {
    return <Outlet />
  }

  if (isPublicRoute(location.pathname)) {
    return <Outlet />
  }

  return <Navigate to="/login" state={{ from: location }} replace />
}