import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import { env } from '../../lib/env.js'

export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({ baseUrl: env.apiBaseUrl }),
  tagTypes: [],
  endpoints: () => ({}),
})
