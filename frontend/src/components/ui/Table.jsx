import { EmptyState } from '../feedback/EmptyState.jsx'
import { cn } from '../../utils/cn.js'

export function Table({ className, children, ...props }) {
  return (
    <div className="w-full overflow-x-auto rounded-xl border border-line bg-surface shadow-card">
      <table
        className={cn('w-full text-left text-sm text-ink', className)}
        {...props}
      >
        {children}
      </table>
    </div>
  )
}

export function TableHeader({ className, children, ...props }) {
  return (
    <thead
      className={cn(
        'border-b border-line bg-canvas/60 text-xs font-semibold uppercase tracking-wider text-muted',
        className,
      )}
      {...props}
    >
      {children}
    </thead>
  )
}

export function TableBody({ className, children, ...props }) {
  return (
    <tbody className={cn('divide-y divide-line/60', className)} {...props}>
      {children}
    </tbody>
  )
}

export function TableFooter({ className, children, ...props }) {
  return (
    <tfoot
      className={cn(
        'border-t border-line bg-canvas/40 text-xs font-medium text-muted',
        className,
      )}
      {...props}
    >
      {children}
    </tfoot>
  )
}

export function TableRow({ className, children, ...props }) {
  return (
    <tr
      className={cn('transition-colors hover:bg-canvas/50', className)}
      {...props}
    >
      {children}
    </tr>
  )
}

export function TableHead({ className, children, scope = 'col', ...props }) {
  return (
    <th
      scope={scope}
      className={cn(
        'px-4 py-3 align-middle font-semibold text-muted first:pl-5 last:pr-5',
        className,
      )}
      {...props}
    >
      {children}
    </th>
  )
}

export function TableCell({ className, children, ...props }) {
  return (
    <td
      className={cn(
        'px-4 py-3.5 align-middle text-ink first:pl-5 last:pr-5',
        className,
      )}
      {...props}
    >
      {children}
    </td>
  )
}

export function TableEmptyState({
  colSpan,
  icon,
  title,
  description,
  action,
  className,
  ...props
}) {
  return (
    <TableRow className="hover:bg-transparent">
      <TableCell colSpan={colSpan} className="p-0 text-center" {...props}>
        <EmptyState
          icon={icon}
          title={title}
          description={description}
          action={action}
          className={className}
        />
      </TableCell>
    </TableRow>
  )
}
