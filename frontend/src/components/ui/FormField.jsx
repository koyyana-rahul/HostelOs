import { cloneElement, isValidElement, useId } from 'react'
import { cn } from '../../utils/cn.js'

function mergeDescribedBy(existingDescribedBy, ...newIds) {
  const existingTokens =
    typeof existingDescribedBy === 'string' && existingDescribedBy.trim()
      ? existingDescribedBy.trim().split(/\s+/)
      : []
  const allTokens = existingTokens.concat(newIds.filter(Boolean))
  const unique = []
  const seen = new Set()

  for (const token of allTokens) {
    if (token && !seen.has(token)) {
      seen.add(token)
      unique.push(token)
    }
  }

  return unique.length > 0 ? unique.join(' ') : undefined
}

/**
 * FormField connects a label, optional hint, and optional error message to an input control.
 *
 * Supported child contract:
 * - A single React element (e.g. <Input />), which is cloned with associated ID, ARIA, and validation props.
 * - A render function: ({ id, 'aria-describedby', invalid, required }) => ReactNode
 *
 * Multiple sibling elements without a single wrapper or render function are rendered as-is and not cloned.
 */
export function FormField({
  id: explicitId,
  label,
  hint,
  error,
  required = false,
  className,
  children,
}) {
  const generatedId = useId()
  const inputId = explicitId || generatedId
  const hintId = hint ? `${inputId}-hint` : undefined
  const errorId = error ? `${inputId}-error` : undefined

  let content
  if (typeof children === 'function') {
    content = children({
      id: inputId,
      'aria-describedby': mergeDescribedBy(undefined, hintId, errorId),
      invalid: Boolean(error),
      required,
    })
  } else if (isValidElement(children)) {
    const childId = children.props.id || inputId
    const mergedDescribedBy = mergeDescribedBy(
      children.props['aria-describedby'],
      hintId,
      errorId,
    )
    content = cloneElement(children, {
      id: childId,
      'aria-describedby': mergedDescribedBy,
      invalid: children.props.invalid ?? Boolean(error),
      required: children.props.required ?? required,
    })
  } else {
    content = children
  }

  const effectiveLabelFor =
    isValidElement(children) && children.props.id
      ? children.props.id
      : inputId

  return (
    <div className={cn('space-y-1.5', className)}>
      {label ? (
        <label
          htmlFor={effectiveLabelFor}
          className="block text-xs font-semibold text-ink"
        >
          {label}
          {required ? (
            <span className="ml-1 text-danger" aria-hidden="true">
              *
            </span>
          ) : null}
        </label>
      ) : null}

      {content}

      {hint ? (
        <p id={hintId} className="text-xs leading-4 text-muted">
          {hint}
        </p>
      ) : null}

      {error ? (
        <p
          id={errorId}
          role="alert"
          className="text-xs font-medium leading-4 text-danger"
        >
          {error}
        </p>
      ) : null}
    </div>
  )
}
