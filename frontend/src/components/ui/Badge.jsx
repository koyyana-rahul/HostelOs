import { cn } from '../../utils/cn.js'

const tones = {
  neutral: 'border-line bg-surface text-muted',
  brand: 'border-brand/15 bg-brand-soft text-brand-strong',
  success: 'border-success/15 bg-success-soft text-success',
  warning: 'border-warning/15 bg-warning-soft text-warning',
  danger: 'border-danger/15 bg-danger-soft text-danger',
}

const dotTones = {
  neutral: 'bg-muted',
  brand: 'bg-brand',
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-danger',
}

export function Badge({ tone = 'neutral', dot = false, className, children }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border px-2.5 py-1 text-xs font-medium',
        tones[tone] ?? tones.neutral,
        className,
      )}
    >
      {dot ? (
        <span
          className={cn(
            'size-1.5 rounded-full',
            dotTones[tone] ?? dotTones.neutral,
          )}
          aria-hidden="true"
        />
      ) : null}
      {children}
    </span>
  )
}
