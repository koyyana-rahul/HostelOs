import { useEffect, useRef } from 'react'
import { X } from 'lucide-react'
import { cn } from '../../utils/cn.js'

export function Modal({ open, title, description, onClose, children }) {
  const overlayRef = useRef(null)

  useEffect(() => {
    if (!open) return undefined
    const handleEscape = (event) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [open, onClose])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
      onClick={(event) => {
        if (event.target === overlayRef.current) onClose()
      }}
    >
      <div
        ref={overlayRef}
        className={cn(
          'w-full max-w-2xl rounded-2xl border border-line bg-surface shadow-card',
          'max-h-[90vh] overflow-y-auto',
        )}
      >
        <div className="sticky top-0 flex items-start justify-between gap-4 border-b border-line/70 px-6 py-4">
          <div>
            <h2 id="modal-title" className="text-lg font-semibold text-ink">
              {title}
            </h2>
            {description ? (
              <p className="mt-1 text-sm text-muted">{description}</p>
            ) : null}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-muted hover:bg-canvas hover:text-ink"
            aria-label="Close dialog"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>
  )
}