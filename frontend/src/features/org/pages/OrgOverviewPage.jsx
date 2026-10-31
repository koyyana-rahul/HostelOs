export default function OrgOverviewPage() {
  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold tracking-[-0.02em] text-ink">Overview</h1>
        <p className="mt-1 text-sm text-muted">Organization overview</p>
      </header>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-line p-5">
          <p className="text-sm text-muted">Properties</p>
          <p className="mt-2 text-2xl font-semibold text-ink">—</p>
        </div>
        <div className="rounded-2xl border border-line p-5">
          <p className="text-sm text-muted">Staff</p>
          <p className="mt-2 text-2xl font-semibold text-ink">—</p>
        </div>
        <div className="rounded-2xl border border-line p-5">
          <p className="text-sm text-muted">Operations</p>
          <p className="mt-2 text-2xl font-semibold text-ink">Planned</p>
        </div>
        <div className="rounded-2xl border border-line p-5">
          <p className="text-sm text-muted">Status</p>
          <p className="mt-2 text-2xl font-semibold text-ink">Active</p>
        </div>
      </div>
    </div>
  )
}
