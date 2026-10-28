import { cn } from '../../utils/cn.js'

const tones = {
  surface: 'border-line/80 bg-surface',
  soft: 'border-brand/20 bg-brand-soft/60',
  brand: 'border-brand bg-brand text-white',
}

export function Card({
  tone = 'surface',
  className,
  children,
  ...props
}) {
  return (
    <div
      className={cn(
        'rounded-2xl border shadow-card',
        tones[tone] ?? tones.surface,
        className,
      )}
      {...props}
    >
      {children}
    </div>
  )
}
