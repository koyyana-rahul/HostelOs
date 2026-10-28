import { cn } from '../../utils/cn.js'

const base =
  'inline-flex items-center justify-center gap-2 rounded-lg text-sm font-semibold transition-colors duration-150 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:pointer-events-none disabled:opacity-50'

const variants = {
  primary: 'bg-brand text-white shadow-sm hover:bg-brand-strong',
  secondary:
    'border border-line bg-surface text-ink hover:border-brand/30 hover:bg-brand-soft',
  ghost: 'text-muted hover:bg-brand-soft hover:text-brand-strong',
}

const sizes = {
  sm: 'h-9 px-3.5',
  md: 'h-10 px-4',
}

export function buttonStyles({ variant = 'primary', size = 'md', className } = {}) {
  return cn(
    base,
    variants[variant] ?? variants.primary,
    sizes[size] ?? sizes.md,
    className,
  )
}
