import { baseApi } from '../../../services/api/baseApi.js'
import { csrfRequest } from '../../../services/api/csrfRequest.js'

export const enquiryApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    listEnquiries: builder.query({
      query: ({ organizationId, status }) => {
        const params = new URLSearchParams()
        if (status) params.set('status', status)
        const query = params.toString() ? `?${params.toString()}` : ''
        return `/v1/organizations/${organizationId}/enquiries${query}`
      },
      providesTags: (result, error, { organizationId }) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: 'Enquiry', id })),
              { type: 'Enquiry', id: `ORG:${organizationId}` },
            ]
          : [{ type: 'Enquiry', id: `ORG:${organizationId}` }],
    }),
    getEnquiry: builder.query({
      query: ({ organizationId, enquiryId }) =>
        `/v1/organizations/${organizationId}/enquiries/${enquiryId}`,
      providesTags: (result, error, { enquiryId }) => [
        { type: 'Enquiry', id: enquiryId },
      ],
    }),
    createEnquiry: builder.mutation({
      async queryFn({ listingId, ...body }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `/v1/listings/${listingId}/enquiries`,
            { method: 'POST', body: JSON.stringify(body) },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: (result) =>
        result
          ? [
              { type: 'Enquiry', id: `ORG:${result.organizationId}` },
              { type: 'Dashboard', id: result.organizationId },
            ]
          : ['Enquiry'],
    }),
    updateEnquiryStatus: builder.mutation({
      async queryFn({ organizationId, enquiryId, status, version }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `/v1/organizations/${organizationId}/enquiries/${enquiryId}/status`,
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
      invalidatesTags: (result, error, { organizationId, enquiryId }) => [
        { type: 'Enquiry', id: enquiryId },
        { type: 'Enquiry', id: `ORG:${organizationId}` },
        { type: 'Dashboard', id: organizationId },
      ],
    }),
  }),
})

export const {
  useListEnquiriesQuery,
  useGetEnquiryQuery,
  useCreateEnquiryMutation,
  useUpdateEnquiryStatusMutation,
} = enquiryApi
