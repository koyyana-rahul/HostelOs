import { Badge } from '../../../components/ui/Badge.jsx'
import { Card } from '../../../components/ui/Card.jsx'

export function MetricCard({
  icon: Icon,
  label,
  value,
  helperText,
  badge,
  badgeTone = 'brand',
  tone = 'surface',
  className,
}) {
  return (
    <Card tone={tone} className={['p-5', className].filter(Boolean).join(' ')}>
      <div className="flex items-start justify-between gap-3">
        {Icon ? (
          <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-soft text-brand">
            <Icon className="size-5" aria-hidden="true" />
          </span>
        ) : null}
        {badge ? (
          <Badge tone={badgeTone} dot>
            {badge}
          </Badge>
        ) : null}
      </div>
      <div className="mt-4">
        <p className="text-xs font-medium text-muted">{label}</p>
        <p className="mt-1 text-2xl font-bold tracking-tight text-ink">{value}</p>
        {helperText ? (
          <p className="mt-1 text-xs text-muted">{helperText}</p>
        ) : null}
      </div>
    </Card>
  )
}
