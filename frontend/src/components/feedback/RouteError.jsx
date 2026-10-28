import { isRouteErrorResponse, useRouteError } from 'react-router-dom'
import { env } from '../../lib/env.js'
import { Button } from '../ui/Button.jsx'
import { ErrorState } from './ErrorState.jsx'

function getErrorDescription(error) {
  if (isRouteErrorResponse(error)) {
    return `${error.status} ${error.statusText}`.trim()
  }

  if (env.isDev && error instanceof Error) {
    return error.message
  }

  return 'An unexpected error interrupted this view. Reload the page or return to the dashboard.'
}

export function RouteError() {
  const error = useRouteError()

  return (
    <main className="grid min-h-dvh place-items-center bg-canvas px-4 py-12">
      <ErrorState
        title="This view could not be rendered"
        description={getErrorDescription(error)}
        action={
          <Button onClick={() => window.location.reload()}>Reload page</Button>
        }
      />
    </main>
  )
}
