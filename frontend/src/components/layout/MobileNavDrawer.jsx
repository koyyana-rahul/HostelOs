import { X } from 'lucide-react'
import { useCallback, useEffect, useRef } from 'react'
import { SidebarContent } from './SidebarContent.jsx'

export function MobileNavDrawer({
  open = false,
  onClose,
  triggerButtonRef,
  children,
}) {
  const dialogRef = useRef(null)
  const closeButtonRef = useRef(null)
  const onCloseRef = useRef(onClose)
  const shouldRestoreFocusRef = useRef(false)
  const isNavigatingRef = useRef(false)

  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  const handleDismiss = () => {
    shouldRestoreFocusRef.current = true
    isNavigatingRef.current = false
    onCloseRef.current?.()
  }

  const handleNavigate = useCallback(() => {
    shouldRestoreFocusRef.current = false
    isNavigatingRef.current = true
    onCloseRef.current?.()
  }, [])

  useEffect(() => {
    if (!open) {
      return undefined
    }

    const trigger = triggerButtonRef?.current
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

    closeButtonRef.current?.focus()

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        event.stopPropagation()
        handleDismiss()
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

      if (shouldRestoreFocusRef.current && !isNavigatingRef.current) {
        const isDesktop =
          typeof window !== 'undefined' &&
          window.matchMedia &&
          window.matchMedia('(min-width: 1024px)').matches

        if (trigger && document.body.contains(trigger) && !isDesktop) {
          trigger.focus()
        }
      }

      shouldRestoreFocusRef.current = false
      isNavigatingRef.current = false
    }
  }, [open, triggerButtonRef])

  if (!open) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      <button
        type="button"
        aria-label="Dismiss navigation"
        tabIndex={-1}
        onClick={handleDismiss}
        className="fixed inset-0 bg-ink/35 backdrop-blur-[2px]"
      />
      <dialog
        ref={dialogRef}
        id="mobile-navigation"
        aria-modal="true"
        aria-label="Application navigation"
        className="relative z-10 m-0 flex h-full w-[min(20rem,86vw)] flex-col border-r border-line bg-surface p-0 shadow-2xl"
      >
        <div className="absolute right-3 top-3 z-10">
          <button
            ref={closeButtonRef}
            type="button"
            aria-label="Close navigation"
            onClick={handleDismiss}
            className="grid size-9 place-items-center rounded-lg text-muted transition-colors hover:bg-canvas hover:text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
          >
            <X className="size-4" aria-hidden="true" />
          </button>
        </div>
        {children ?? <SidebarContent onNavigate={handleNavigate} />}
      </dialog>
    </div>
  )
}
