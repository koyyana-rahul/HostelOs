import { Compass, ShieldAlert } from 'lucide-react'
import { isRouteErrorResponse, Link, useRouteError } from 'react-router-dom'
import { env } from '../../lib/env.js'
import { Button } from '../ui/Button.jsx'
import { buttonStyles } from '../ui/buttonStyles.js'
import { Card } from '../ui/Card.jsx'
import { ErrorState } from './ErrorState.jsx'

export function RouteError() {
  const error = useRouteError()

  if (isRouteErrorResponse(error) && error.status === 403) {
    return (
      <main className="grid min-h-dvh place-items-center bg-canvas px-4 py-12">
        <Card className="mx-auto max-w-2xl">
          <div className="flex flex-col items-center px-6 py-14 text-center">
            <div className="mb-5 grid size-14 place-items-center rounded-2xl bg-danger-soft text-danger">
              <ShieldAlert className="size-6" aria-hidden="true" />
            </div>
            <h1 className="text-2xl font-semibold tracking-tight text-ink">
              Access denied
            </h1>
            <p className="mt-2 max-w-md text-sm leading-6 text-muted">
              You do not have permission to access this section. If you believe this is an error, contact your administrator.
            </p>
            <div className="mt-6">
              <Link to="/" className={buttonStyles()}>
                Back to overview
              </Link>
            </div>
          </div>
        </Card>
      </main>
    )
  }

  if (isRouteErrorResponse(error) && error.status === 404) {
    return (
      <main className="grid min-h-dvh place-items-center bg-canvas px-4 py-12">
        <Card className="mx-auto max-w-2xl">
          <div className="flex flex-col items-center px-6 py-14 text-center">
            <div className="mb-5 grid size-14 place-items-center rounded-2xl bg-brand-soft text-brand">
              <Compass className="size-6" aria-hidden="true" />
            </div>
            <h1 className="text-2xl font-semibold tracking-tight text-ink">
              Page not found
            </h1>
            <p className="mt-2 max-w-md text-sm leading-6 text-muted">
              The page you are looking for does not exist or has moved.
            </p>
            <div className="mt-6">
              <Link to="/" className={buttonStyles()}>
                Back to overview
              </Link>
            </div>
          </div>
        </Card>
      </main>
    )
  }

  return (
    <main className="grid min-h-dvh place-items-center bg-canvas px-4 py-12">
      <ErrorState
        title="This view could not be rendered"
        description={
          env.isDev && error instanceof Error
            ? error.message
            : 'An unexpected error interrupted this view. Reload the page or return to the dashboard.'
        }
        action={
          <Button onClick={() => window.location.reload()}>Reload page</Button>
        }
      />
    </main>
  )
}

