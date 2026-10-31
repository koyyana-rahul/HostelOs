import { X } from 'lucide-react'
import { useEffect, useId, useRef } from 'react'
import { cn } from '../../utils/cn.js'

export function Dialog({
  open = false,
  onClose,
  title,
  description,
  children,
  className,
  'aria-label': ariaLabel,
  'aria-labelledby': ariaLabelledBy,
  'aria-describedby': ariaDescribedBy,
}) {
  const dialogRef = useRef(null)
  const previousActiveElementRef = useRef(null)
  const onCloseRef = useRef(onClose)
  const generatedId = useId()

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  const titleId = title ? `${generatedId}-title` : undefined
  const descriptionId = description ? `${generatedId}-description` : undefined

  useEffect(() => {
    if (!open) {
      return undefined
    }

    previousActiveElementRef.current =
      document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null

    const dialog = dialogRef.current
    if (dialog) {
      if (typeof dialog.showModal === 'function') {
        if (!dialog.open) {
          dialog.showModal()
        }
      } else if (!dialog.hasAttribute('open')) {
        dialog.setAttribute('open', '')
      }
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        event.stopPropagation()
        onCloseRef.current?.()
      }
    }

    window.addEventListener('keydown', handleKeyDown)

    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = previousOverflow

      if (dialog) {
        if (typeof dialog.close === 'function') {
          if (dialog.open) {
            dialog.close()
          }
        } else {
          dialog.removeAttribute('open')
        }
      }

      if (previousActiveElementRef.current?.focus) {
        previousActiveElementRef.current.focus()
      }
    }
  }, [open])

  if (!open) {
    return null
  }

  const handleBackdropClick = (event) => {
    if (event.target === dialogRef.current) {
      const rect = dialogRef.current.getBoundingClientRect()
      const isInside =
        rect.top <= event.clientY &&
        event.clientY <= rect.top + rect.height &&
        rect.left <= event.clientX &&
        event.clientX <= rect.left + rect.width

      if (!isInside) {
        onCloseRef.current?.()
      }
    }
  }

  return (
    <dialog
      ref={dialogRef}
      onClick={handleBackdropClick}
      aria-label={ariaLabel}
      aria-labelledby={ariaLabelledBy || titleId}
      aria-describedby={ariaDescribedBy || descriptionId}
      className={cn(
        'fixed inset-0 z-50 m-auto flex max-h-[85vh] w-[min(32rem,calc(100vw-2rem))] flex-col rounded-2xl border border-line bg-surface p-0 text-ink shadow-2xl backdrop:bg-ink/40 backdrop:backdrop-blur-[2px]',
        className,
      )}
    >
      <div className="flex items-start justify-between gap-4 border-b border-line/70 px-6 py-4">
        <div>
          {title ? (
            <h2 id={titleId} className="text-base font-semibold text-ink">
              {title}
            </h2>
          ) : null}
          {description ? (
            <p id={descriptionId} className="mt-1 text-xs leading-5 text-muted">
              {description}
            </p>
          ) : null}
        </div>
        <button
          type="button"
          aria-label="Close dialog"
          onClick={() => onCloseRef.current?.()}
          className="grid size-8 shrink-0 place-items-center rounded-lg text-muted transition-colors hover:bg-canvas hover:text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
          <X className="size-4" aria-hidden="true" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-6 py-4">{children}</div>
    </dialog>
  )
}

export function DialogHeader({ title, description, children, className }) {
  return (
    <div className={cn('mb-4 space-y-1', className)}>
      {title ? (
        <h3 className="text-sm font-semibold text-ink">{title}</h3>
      ) : null}
      {description ? (
        <p className="text-xs leading-5 text-muted">{description}</p>
      ) : null}
      {children}
    </div>
  )
}

export function DialogFooter({ className, children }) {
  return (
    <div
      className={cn(
        'mt-4 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end sm:gap-2.5',
        className,
      )}
    >
      {children}
    </div>
  )
}
