import { Compass } from 'lucide-react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../components/feedback/EmptyState.jsx'
import { buttonStyles } from '../components/ui/buttonStyles.js'
import { Card } from '../components/ui/Card.jsx'
import { useDocumentTitle } from '../hooks/useDocumentTitle.js'

export default function NotFoundPage() {
  useDocumentTitle('Page not found')

  return (
    <Card className="mx-auto max-w-2xl">
      <EmptyState
        icon={Compass}
        title="Page not found"
        description="The page you are looking for does not exist or has moved. Return to the overview to continue."
        action={
          <Link to="/" className={buttonStyles()}>
            Back to overview
          </Link>
        }
      />
    </Card>
  )
}
