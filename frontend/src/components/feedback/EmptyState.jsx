import { cn } from '../../utils/cn.js'

export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
}) {
  return (
    <div
      className={cn(
        'flex flex-col items-center px-6 py-14 text-center',
        className,
      )}
    >
      {Icon ? (
        <div className="mb-5 grid size-14 place-items-center rounded-2xl bg-brand-soft text-brand">
          <Icon className="size-6" aria-hidden="true" />
        </div>
      ) : null}
      <h1 className="text-2xl font-semibold tracking-tight text-ink">
        {title}
      </h1>
      {description ? (
        <p className="mt-2 max-w-md text-sm leading-6 text-muted">
          {description}
        </p>
      ) : null}
      {action ? <div className="mt-6">{action}</div> : null}
    </div>
  )
}
