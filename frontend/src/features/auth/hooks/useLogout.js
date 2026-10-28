import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppDispatch } from '../../../hooks/useAppDispatch.js'
import { authApi, useLazyGetCsrfTokenQuery, useLogoutMutation } from '../api/authApi.js'
import { sessionCleared } from '../authSlice.js'
import { getLogoutErrorMessage } from '../lib/authErrors.js'

export function useLogout() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [getCsrfToken] = useLazyGetCsrfTokenQuery()
  const [logoutRequest] = useLogoutMutation()
  const [isSigningOut, setIsSigningOut] = useState(false)
  const [error, setError] = useState(null)

  const signOut = async () => {
    if (isSigningOut) {
      return
    }

    setError(null)
    setIsSigningOut(true)

    try {
      const csrf = await getCsrfToken(undefined, false).unwrap()
      await logoutRequest(csrf).unwrap()
    } catch (requestError) {
      if (requestError?.status !== 401) {
        setError(getLogoutErrorMessage(requestError))
        setIsSigningOut(false)
        return
      }
    }

    setIsSigningOut(false)
    dispatch(authApi.util.resetApiState())
    dispatch(sessionCleared())
    navigate('/login', { replace: true })
  }

  return { signOut, isSigningOut, error }
}
