import { baseApi } from '../../../services/api/baseApi.js'

export const systemApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getSystemInfo: builder.query({
      query: () => '/v1/system/info',
    }),
  }),
})

export const { useGetSystemInfoQuery } = systemApi
