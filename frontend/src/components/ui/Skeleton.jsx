import { cn } from '../../utils/cn.js'

const variants = {
  text: 'h-4 w-full rounded',
  circular: 'rounded-full',
  rectangular: 'rounded-xl',
}

export function Skeleton({ variant = 'text', className, ...props }) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        'animate-pulse bg-muted/15 motion-reduce:animate-none',
        variants[variant] ?? variants.text,
        className,
      )}
      {...props}
    />
  )
}
