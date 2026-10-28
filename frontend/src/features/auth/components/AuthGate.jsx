import { Building2, LoaderCircle, RefreshCw } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { ErrorState } from '../../../components/feedback/ErrorState.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Card } from '../../../components/ui/Card.jsx'
import { useAppDispatch } from '../../../hooks/useAppDispatch.js'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { useGetCurrentUserQuery } from '../api/authApi.js'
import {
  selectAuthStatus,
  sessionCheckFailed,
  sessionCheckStarted,
  sessionCleared,
  sessionEstablished,
} from '../authSlice.js'

function SessionLoadingScreen() {
  return (
    <main className="grid min-h-dvh place-items-center bg-canvas px-4 py-12">
      <div
        role="status"
        aria-live="polite"
        className="flex w-full max-w-sm flex-col items-center rounded-2xl border border-line bg-surface px-6 py-10 text-center shadow-card"
      >
        <span className="grid size-12 place-items-center rounded-2xl bg-brand-soft text-brand">
          <Building2 className="size-6" aria-hidden="true" />
        </span>
        <LoaderCircle
          className="mt-6 size-5 animate-spin text-brand"
          aria-hidden="true"
        />
        <p className="mt-3 text-sm font-semibold text-ink">
          Checking your session
        </p>
        <p className="mt-1 text-xs leading-5 text-muted">
          Confirming access with the HostelOS backend.
        </p>
      </div>
    </main>
  )
}

function SessionUnavailableScreen({ onRetry }) {
  return (
    <main className="grid min-h-dvh place-items-center bg-canvas px-4 py-12">
      <Card className="max-w-xl">
        <ErrorState
          title="Authentication unavailable"
          description="The HostelOS backend could not confirm your session. Check that it is running, then try again."
          action={
            <Button variant="secondary" onClick={onRetry}>
              <RefreshCw className="size-4" aria-hidden="true" />
              Try again
            </Button>
          }
        />
      </Card>
    </main>
  )
}

export function AuthGate() {
  const dispatch = useAppDispatch()
  const status = useAppSelector(selectAuthStatus)
  const [attempt, setAttempt] = useState(0)
  const { data, error } = useGetCurrentUserQuery(attempt, {
    skip: status !== 'checking',
  })

  useEffect(() => {
    if (data) {
      dispatch(sessionEstablished(data))
      return
    }

    if (error?.status === 401) {
      dispatch(sessionCleared())
      return
    }

    if (error) {
      dispatch(sessionCheckFailed())
    }
  }, [data, error, dispatch])

  const retry = () => {
    dispatch(sessionCheckStarted())
    setAttempt((value) => value + 1)
  }

  if (status === 'unavailable') {
    return <SessionUnavailableScreen onRetry={retry} />
  }

  if (status === 'checking') {
    return <SessionLoadingScreen />
  }

  return <Outlet />
}
