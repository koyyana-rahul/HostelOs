import { baseApi } from '../../../services/api/baseApi.js'
import { csrfRequest } from '../../../services/api/csrfRequest.js'

function buildListQuery({ organizationId, status }) {
  const params = new URLSearchParams()
  if (status) params.set('status', status)
  const query = params.toString() ? `?${params.toString()}` : ''
  return `/v1/organizations/${organizationId}/listings${query}`
}

function buildPublicQuery({
  q,
  city,
  type,
  minPrice,
  maxPrice,
  page = 0,
  size = 12,
} = {}) {
  const params = new URLSearchParams()
  if (q) params.set('q', q)
  if (city) params.set('city', city)
  if (type) params.set('type', type)
  if (minPrice !== '' && minPrice !== null && minPrice !== undefined) {
    params.set('minPrice', minPrice)
  }
  if (maxPrice !== '' && maxPrice !== null && maxPrice !== undefined) {
    params.set('maxPrice', maxPrice)
  }
  params.set('page', String(page))
  params.set('size', String(size))
  return `/v1/public/listings?${params.toString()}`
}

export const listingApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    listListings: builder.query({
      query: buildListQuery,
      providesTags: (result, error, { organizationId }) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: 'Listing', id })),
              { type: 'Listing', id: `ORG:${organizationId}` },
            ]
          : [{ type: 'Listing', id: `ORG:${organizationId}` }],
    }),
    getListing: builder.query({
      query: ({ organizationId, listingId }) =>
        `/v1/organizations/${organizationId}/listings/${listingId}`,
      providesTags: (result, error, { listingId }) => [
        { type: 'Listing', id: listingId },
      ],
    }),
    createListing: builder.mutation({
      async queryFn({ organizationId, ...body }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `/v1/organizations/${organizationId}/listings`,
            { method: 'POST', body: JSON.stringify(body) },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: (result, error, { organizationId }) => [
        { type: 'Listing', id: `ORG:${organizationId}` },
        { type: 'Listing', id: 'PUBLIC' },
        { type: 'Dashboard', id: organizationId },
      ],
    }),
    updateListing: builder.mutation({
      async queryFn({ organizationId, listingId, ...body }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `/v1/organizations/${organizationId}/listings/${listingId}`,
            { method: 'PUT', body: JSON.stringify(body) },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: (result, error, { organizationId, listingId }) => [
        { type: 'Listing', id: listingId },
        { type: 'Listing', id: `ORG:${organizationId}` },
        { type: 'Listing', id: 'PUBLIC' },
        { type: 'Dashboard', id: organizationId },
      ],
    }),
    changeListingStatus: builder.mutation({
      async queryFn({ organizationId, listingId, status, version }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `/v1/organizations/${organizationId}/listings/${listingId}/status`,
            {
              method: 'PATCH',
              body: JSON.stringify({ status, version }),
            },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: (result, error, { organizationId, listingId }) => [
        { type: 'Listing', id: listingId },
        { type: 'Listing', id: `ORG:${organizationId}` },
        { type: 'Listing', id: 'PUBLIC' },
        { type: 'Dashboard', id: organizationId },
      ],
    }),
    searchPublicListings: builder.query({
      query: buildPublicQuery,
      providesTags: (result) =>
        result
          ? [
              { type: 'Listing', id: 'PUBLIC' },
              ...(result.content || []).map(({ id }) => ({
                type: 'Listing',
                id,
              })),
            ]
          : [{ type: 'Listing', id: 'PUBLIC' }],
    }),
    getPublicListing: builder.query({
      query: (listingId) => `/v1/public/listings/${listingId}`,
      providesTags: (result, error, listingId) => [
        { type: 'Listing', id: listingId },
      ],
    }),
  }),
})

export const {
  useListListingsQuery,
  useGetListingQuery,
  useCreateListingMutation,
  useUpdateListingMutation,
  useChangeListingStatusMutation,
  useSearchPublicListingsQuery,
  useGetPublicListingQuery,
} = listingApi
