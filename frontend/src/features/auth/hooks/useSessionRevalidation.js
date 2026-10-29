import { useEffect } from 'react'
import { useAppDispatch } from '../../../hooks/useAppDispatch.js'
import { useAppSelector } from '../../../hooks/useAppSelector.js'
import { useLazyGetCurrentUserQuery } from '../api/authApi.js'
import { selectAuthStatus, sessionEstablished } from '../authSlice.js'

export function useSessionRevalidation() {
  const dispatch = useAppDispatch()
  const status = useAppSelector(selectAuthStatus)
  const [getCurrentUser] = useLazyGetCurrentUserQuery()

  useEffect(() => {
    if (status !== 'authenticated') {
      return undefined
    }

    const revalidate = () => {
      if (document.visibilityState !== 'visible') {
        return
      }

      getCurrentUser(undefined, false)
        .unwrap()
        .then((user) => dispatch(sessionEstablished(user)))
        .catch(() => undefined)
    }

    window.addEventListener('focus', revalidate)
    document.addEventListener('visibilitychange', revalidate)

    return () => {
      window.removeEventListener('focus', revalidate)
      document.removeEventListener('visibilitychange', revalidate)
    }
  }, [status, getCurrentUser, dispatch])
}
