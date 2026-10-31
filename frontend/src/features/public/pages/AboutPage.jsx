import { Card } from '../../../components/ui/Card.jsx'

export default function AboutPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
      <div>
        <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-brand">About</p>
        <h1 className="mt-3 text-3xl font-semibold tracking-[-0.02em] text-ink sm:text-4xl">About HostelOS</h1>
        <p className="mt-3 text-lg text-muted">
          HostelOS is a two-sided platform for hostel and PG discovery and management.
        </p>
      </div>
      <Card className="mt-8 p-6 sm:p-8">
        <p className="text-sm leading-6 text-muted">
          We connect students and working professionals with suitable accommodations, while giving hostel and PG owners the tools to digitize and manage their operations with clarity and control.
        </p>
      </Card>
    </div>
  )
}
