import { AlertTriangle } from 'lucide-react'
import { cn } from '../../utils/cn.js'

export function ErrorState({
  title = 'Something went wrong',
  description = 'An unexpected error interrupted this view. Try again in a moment.',
  action,
  className,
}) {
  return (
    <div
      role="alert"
      className={cn(
        'flex max-w-lg flex-col items-center px-6 py-12 text-center',
        className,
      )}
    >
      <div className="mb-5 grid size-14 place-items-center rounded-2xl bg-danger-soft text-danger">
        <AlertTriangle className="size-6" aria-hidden="true" />
      </div>
      <h1 className="text-2xl font-semibold tracking-tight text-ink">
        {title}
      </h1>
      <p className="mt-2 text-sm leading-6 text-muted">{description}</p>
      {action ? <div className="mt-6">{action}</div> : null}
    </div>
  )
}
