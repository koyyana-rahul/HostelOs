import { useEffect } from 'react'
import { env } from '../lib/env.js'

export function useDocumentTitle(title) {
  useEffect(() => {
    document.title = title ? `${title} · ${env.appName}` : env.appName
  }, [title])
}
