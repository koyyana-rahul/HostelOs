import { authApi } from '../../features/auth/api/authApi.js'
import { env } from '../../lib/env.js'

export async function csrfRequest(path, init, dispatch) {
  const csrf = await dispatch(
    authApi.endpoints.getCsrfToken.initiate(undefined, { forceRefetch: true }),
  ).unwrap()

  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    ...init,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers || {}),
      [csrf.headerName]: csrf.token,
    },
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    const error = new Error(
      body?.message || `Request failed with status ${response.status}`,
    )
    error.status = response.status
    error.data = body
    throw error
  }

  if (response.status === 204) {
    return null
  }

  const text = await response.text()
  return text ? JSON.parse(text) : null
}
