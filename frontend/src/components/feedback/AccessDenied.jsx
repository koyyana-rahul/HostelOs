import { ShieldAlert } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js'
import { buttonStyles } from '../ui/buttonStyles.js'
import { Card } from '../ui/Card.jsx'

export function AccessDenied({
  title = 'Access denied',
  description = 'You do not have permission to access this section. If you believe this is an error, contact your administrator.',
  action,
}) {
  useDocumentTitle(title)

  return (
    <Card className="mx-auto max-w-2xl">
      <div className="flex flex-col items-center px-6 py-14 text-center">
        <div className="mb-5 grid size-14 place-items-center rounded-2xl bg-danger-soft text-danger">
          <ShieldAlert className="size-6" aria-hidden="true" />
        </div>
        <h1 className="text-2xl font-semibold tracking-tight text-ink">
          {title}
        </h1>
        <p className="mt-2 max-w-md text-sm leading-6 text-muted">
          {description}
        </p>
        <div className="mt-6">
          {action || (
            <Link to="/" className={buttonStyles()}>
              Back to overview
            </Link>
          )}
        </div>
      </div>
    </Card>
  )
}

export default AccessDenied
