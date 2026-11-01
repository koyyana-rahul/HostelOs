import { baseApi } from '../../../services/api/baseApi.js'
import { authApi } from '../../auth/api/authApi.js'
import { env } from '../../../lib/env.js'

async function fetchWithCsrf(path, init, dispatch) {
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
    const error = new Error(body?.message || `Request failed with status ${response.status}`)
    error.status = response.status
    error.data = body
    throw error
  }

  return response.json()
}

export const customerApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    searchListings: builder.query({
      query: (params = {}) => {
        const search = new URLSearchParams()
        if (params.query) search.set('query', params.query)
        if (params.city) search.set('city', params.city)
        if (params.accommodationType) search.set('accommodationType', params.accommodationType)
        if (params.minPrice) search.set('minPrice', String(params.minPrice))
        if (params.maxPrice) search.set('maxPrice', String(params.maxPrice))
        if (params.sort) search.set('sort', params.sort)
        if (params.page) search.set('page', String(params.page))
        const query = search.toString() ? `?${search.toString()}` : ''
        return `/v1/listings/public${query}`
      },
      providesTags: ['Listing'],
    }),
    getListing: builder.query({
      query: (listingId) => `/v1/listings/public/${listingId}`,
      providesTags: (result, error, id) => [{ type: 'Listing', id }],
    }),
    checkAvailability: builder.query({
      query: ({ listingId, ...params }) => {
        const search = new URLSearchParams()
        if (params.checkIn) search.set('checkIn', params.checkIn)
        if (params.checkOut) search.set('checkOut', params.checkOut)
        if (params.guests) search.set('guests', String(params.guests))
        if (params.roomType) search.set('roomType', params.roomType)
        const query = search.toString() ? `?${search.toString()}` : ''
        return `/v1/listings/${listingId}/availability${query}`
      },
      providesTags: (result, error, { listingId }) => [{ type: 'Listing', id: listingId }],
    }),
    createEnquiry: builder.mutation({
      async queryFn({ listingId, ...body }, { dispatch }) {
        try {
          const data = await fetchWithCsrf(
            `/v1/listings/${listingId}/enquiries`,
            { method: 'POST', body: JSON.stringify(body) },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: ['Enquiry'],
    }),
    listMyEnquiries: builder.query({
      query: (params = {}) => {
        const search = new URLSearchParams()
        if (params.status) search.set('status', params.status)
        if (params.page) search.set('page', String(params.page))
        const query = search.toString() ? `?${search.toString()}` : ''
        return `/v1/customers/me/enquiries${query}`
      },
      providesTags: ['Enquiry'],
    }),
    getEnquiry: builder.query({
      query: (enquiryId) => `/v1/customers/me/enquiries/${enquiryId}`,
      providesTags: (result, error, id) => [{ type: 'Enquiry', id }],
    }),
    getMyProfile: builder.query({
      query: () => '/v1/customers/me',
      providesTags: ['Customer'],
    }),
    updateMyProfile: builder.mutation({
      async queryFn(body, { dispatch }) {
        try {
          const data = await fetchWithCsrf(
            '/v1/customers/me',
            { method: 'PUT', body: JSON.stringify(body) },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: ['Customer'],
    }),
  }),
})

export const {
  useSearchListingsQuery,
  useGetListingQuery,
  useCheckAvailabilityQuery,
  useCreateEnquiryMutation,
  useListMyEnquiriesQuery,
  useGetEnquiryQuery,
  useGetMyProfileQuery,
  useUpdateMyProfileMutation,
} = customerApi