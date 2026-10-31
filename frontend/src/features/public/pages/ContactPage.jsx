import { Card } from '../../../components/ui/Card.jsx'
import { Button } from '../../../components/ui/Button.jsx'
import { Link } from 'react-router-dom'

export default function ContactPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
      <div>
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">Contact</p>
        <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">Get in touch</h1>
        <p className="mt-3 text-lg text-muted">Questions about discovery or managing your property?</p>
      </div>
      <Card className="mt-8 p-6 sm:p-8">
        <p className="text-sm text-muted">For account access and support, sign in to your account.</p>
        <div className="mt-4 flex gap-2">
          <Button as={Link} to="/login" size="sm">
            Sign in
          </Button>
          <Button as={Link} to="/register" variant="secondary" size="sm">
            Register
          </Button>
        </div>
      </Card>
    </div>
  )
}