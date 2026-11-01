import { baseApi } from '../../../services/api/baseApi.js'

export const authApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getCurrentUser: builder.query({
      query: () => '/v1/auth/me',
      providesTags: ['Session'],
    }),
    getCsrfToken: builder.query({
      query: () => '/v1/auth/csrf',
      keepUnusedDataFor: 0,
    }),
    login: builder.mutation({
      query: ({ loginIdentifier, password, csrf }) => ({
        url: '/v1/auth/login',
        method: 'POST',
        body: { loginIdentifier, password },
        headers: {
          [csrf.headerName]: csrf.token,
        },
      }),
      invalidatesTags: ['Session', 'Membership', 'Organization'],
    }),
    register: builder.mutation({
      query: ({ loginIdentifier, displayName, password, csrf }) => ({
        url: '/v1/auth/register',
        method: 'POST',
        body: { loginIdentifier, displayName, password },
        headers: {
          [csrf.headerName]: csrf.token,
        },
      }),
    }),
    registerOwner: builder.mutation({
      query: ({ loginIdentifier, displayName, password, csrf }) => ({
        url: '/v1/auth/register/owner',
        method: 'POST',
        body: { loginIdentifier, displayName, password },
        headers: {
          [csrf.headerName]: csrf.token,
        },
      }),
    }),
    logout: builder.mutation({
      query: (csrf) => ({
        url: '/v1/auth/logout',
        method: 'POST',
        headers: {
          [csrf.headerName]: csrf.token,
        },
      }),
      invalidatesTags: ['Session', 'Membership', 'Organization'],
    }),
    getMemberships: builder.query({
      query: () => '/v1/auth/me/memberships',
      providesTags: ['Membership'],
    }),
  }),
})

export const {
  useGetCurrentUserQuery,
  useLazyGetCurrentUserQuery,
  useLazyGetCsrfTokenQuery,
  useLoginMutation,
  useLogoutMutation,
  useRegisterMutation,
  useRegisterOwnerMutation,
  useGetMembershipsQuery,
  useLazyGetMembershipsQuery,
} = authApi
