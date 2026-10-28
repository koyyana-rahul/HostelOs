import { LoaderCircle } from 'lucide-react'
import { cn } from '../../utils/cn.js'

export function PageLoader({ label = 'Loading page', className }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        'flex min-h-[60vh] flex-col items-center justify-center gap-3',
        className,
      )}
    >
      <LoaderCircle
        className="size-6 animate-spin text-brand"
        aria-hidden="true"
      />
      <span className="text-sm text-muted">{label}</span>
    </div>
  )
}
