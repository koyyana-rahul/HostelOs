import { baseApi } from '../../../services/api/baseApi.js'
import { csrfRequest } from '../../../services/api/csrfRequest.js'

export const customerApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    listMyEnquiries: builder.query({
      query: (params = {}) => {
        const search = new URLSearchParams()
        if (params.status) search.set('status', params.status)
        if (params.page !== undefined) search.set('page', String(params.page))
        if (params.size !== undefined) search.set('size', String(params.size))
        const query = search.toString() ? `?${search.toString()}` : ''
        return `/v1/customers/me/enquiries${query}`
      },
      providesTags: (result) =>
        result?.content
          ? [
              ...result.content.map(({ id }) => ({ type: 'Enquiry', id })),
              { type: 'Enquiry', id: 'CUSTOMER' },
            ]
          : [{ type: 'Enquiry', id: 'CUSTOMER' }],
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
          const data = await csrfRequest(
            '/v1/customers/me',
            { method: 'PUT', body: JSON.stringify(body) },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: ['Customer', 'Session'],
    }),
  }),
})

export const {
  useListMyEnquiriesQuery,
  useGetEnquiryQuery,
  useGetMyProfileQuery,
  useUpdateMyProfileMutation,
} = customerApi
