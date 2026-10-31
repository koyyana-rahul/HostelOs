import { baseApi } from '../../../services/api/baseApi.js'

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
    getOrganization: builder.query({
      query: (organizationId) => `/v1/organizations/${organizationId}`,
      providesTags: (result, error, id) => [{ type: 'Organization', id }],
    }),
    createOrganization: builder.mutation({
      query: (body) => ({
        url: '/v1/organizations',
        method: 'POST',
        body,
      }),
      invalidatesTags: [{ type: 'Organization', id: 'LIST' }, 'Membership'],
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
  useGetOrganizationQuery,
  useCreateOrganizationMutation,
  useListOrganizationMembershipsQuery,
} = organizationApi
