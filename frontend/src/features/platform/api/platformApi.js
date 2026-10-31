import { baseApi } from '../../../services/api/baseApi.js'

export const platformApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getPlatformStats: builder.query({
      query: () => '/v1/platform/stats',
      providesTags: ['Organization', 'Session'],
    }),
    getPlatformUsers: builder.query({
      query: (params = {}) => {
        const searchParams = new URLSearchParams()
        if (params.search) searchParams.set('search', params.search)
        if (params.page !== undefined) searchParams.set('page', params.page)
        if (params.size !== undefined) searchParams.set('size', params.size)
        const queryString = searchParams.toString()
        return `/v1/platform/users${queryString ? `?${queryString}` : ''}`
      },
      providesTags: ['Session'],
    }),
    getPlatformUser: builder.query({
      query: (userId) => `/v1/platform/users/${userId}`,
      providesTags: (result, error, id) => [{ type: 'Session', id }],
    }),
    suspendOrganization: builder.mutation({
      query: (organizationId) => ({
        url: `/v1/platform/organizations/${organizationId}/suspend`,
        method: 'POST',
      }),
      invalidatesTags: [{ type: 'Organization', id: 'LIST' }, 'Organization'],
    }),
    reactivateOrganization: builder.mutation({
      query: (organizationId) => ({
        url: `/v1/platform/organizations/${organizationId}/reactivate`,
        method: 'POST',
      }),
      invalidatesTags: [{ type: 'Organization', id: 'LIST' }, 'Organization'],
    }),
  }),
})

export const {
  useGetPlatformStatsQuery,
  useGetPlatformUsersQuery,
  useGetPlatformUserQuery,
  useSuspendOrganizationMutation,
  useReactivateOrganizationMutation,
} = platformApi
