import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import { sessionCleared } from '../../features/auth/authSlice.js'
import { env } from '../../lib/env.js'

const rawBaseQuery = fetchBaseQuery({
  baseUrl: env.apiBaseUrl,
  credentials: 'include',
})

const baseQueryWithSessionHandling = async (args, api, extraOptions) => {
  const result = await rawBaseQuery(args, api, extraOptions)

  if (result.error?.status === 401) {
    const url = typeof args === 'string' ? args : args?.url

    if (!url?.includes('/v1/auth/login')) {
      api.dispatch(sessionCleared())
    }
  }

  return result
}

export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery: baseQueryWithSessionHandling,
  tagTypes: ['Session', 'Organization', 'Membership'],
  endpoints: () => ({}),
})
