import { Activity, LogIn, LogOut, Wrench } from 'lucide-react'

const activityConfig = {
  CHECK_IN: {
    icon: LogIn,
    iconWrapper: 'bg-success-soft text-success',
  },
  CHECK_OUT: {
    icon: LogOut,
    iconWrapper: 'bg-brand-soft text-brand',
  },
  MAINTENANCE: {
    icon: Wrench,
    iconWrapper: 'bg-warning-soft text-warning',
  },
}

export function ActivityFeed({ activities = [] }) {
  if (activities.length === 0) {
    return (
      <p className="py-6 text-center text-xs text-muted">
        No recent activities recorded.
      </p>
    )
  }

  return (
    <ul
      aria-label="Recent activity feed"
      className="divide-y divide-line/60"
    >
      {activities.map((item) => {
        const config = activityConfig[item.type] ?? {
          icon: Activity,
          iconWrapper: 'bg-canvas text-muted',
        }
        const Icon = config.icon

        return (
          <li
            key={item.id}
            className="flex items-start gap-3.5 py-3.5 first:pt-1 last:pb-1"
          >
            <span
              className={`grid size-9 shrink-0 place-items-center rounded-xl ${config.iconWrapper}`}
            >
              <Icon className="size-4" aria-hidden="true" />
            </span>
            <div className="min-w-0 flex-1">
              <div className="flex items-baseline justify-between gap-2">
                <p className="truncate text-sm font-semibold text-ink">
                  {item.title}
                </p>
                <span className="shrink-0 text-xs text-muted">
                  {item.timestamp}
                </span>
              </div>
              <p className="mt-0.5 truncate text-xs text-muted">
                {item.detail}
              </p>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
