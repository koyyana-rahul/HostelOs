import { baseApi } from '../../../services/api/baseApi.js'
import { csrfRequest } from '../../../services/api/csrfRequest.js'

export const organizationApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    listOrganizations: builder.query({
      query: () => '/v1/organizations',
      providesTags: (result) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: 'Organization', id })),
              { type: 'Organization', id: 'LIST' },
            ]
          : [{ type: 'Organization', id: 'LIST' }],
    }),
    listMyOrganizations: builder.query({
      query: () => '/v1/organizations/mine',
      providesTags: (result) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: 'Organization', id })),
              { type: 'Organization', id: 'MINE' },
              { type: 'Organization', id: 'LIST' },
            ]
          : [
              { type: 'Organization', id: 'MINE' },
              { type: 'Organization', id: 'LIST' },
            ],
    }),
    getOrganization: builder.query({
      query: (organizationId) => `/v1/organizations/${organizationId}`,
      providesTags: (result, error, id) => [{ type: 'Organization', id }],
    }),
    getOrganizationDashboard: builder.query({
      query: (organizationId) =>
        `/v1/organizations/${organizationId}/dashboard`,
      providesTags: (result, error, id) => [{ type: 'Dashboard', id }],
    }),
    createOrganization: builder.mutation({
      async queryFn({ name, slug }, { dispatch }) {
        try {
          const data = await csrfRequest(
            '/v1/organizations',
            {
              method: 'POST',
              body: JSON.stringify({ name, slug }),
            },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: (result) => [
        { type: 'Organization', id: 'LIST' },
        { type: 'Organization', id: 'MINE' },
        'Membership',
        ...(result?.id
          ? [{ type: 'Dashboard', id: result.id }]
          : []),
      ],
    }),
    updateOrganization: builder.mutation({
      async queryFn({ organizationId, name }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `/v1/organizations/${organizationId}`,
            {
              method: 'PATCH',
              body: JSON.stringify({ name }),
            },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: (result, error, { organizationId }) => [
        { type: 'Organization', id: organizationId },
        { type: 'Organization', id: 'LIST' },
        { type: 'Organization', id: 'MINE' },
        { type: 'Dashboard', id: organizationId },
      ],
    }),
    listOrganizationMemberships: builder.query({
      query: (organizationId) => `/v1/organizations/${organizationId}/memberships`,
      providesTags: (result, error, id) => [
        { type: 'Membership', id: `ORG:${id}` },
        ...(result?.map((m) => ({ type: 'Membership', id: m.id })) || []),
      ],
    }),
  }),
})

export const {
  useListOrganizationsQuery,
  useListMyOrganizationsQuery,
  useLazyListMyOrganizationsQuery,
  useGetOrganizationQuery,
  useGetOrganizationDashboardQuery,
  useCreateOrganizationMutation,
  useUpdateOrganizationMutation,
  useListOrganizationMembershipsQuery,
} = organizationApi
