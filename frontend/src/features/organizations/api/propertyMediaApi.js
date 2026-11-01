import { baseApi } from '../../../services/api/baseApi.js'
import { csrfRequest } from '../../../services/api/csrfRequest.js'

const mediaPath = ({ organizationId, propertyId }) =>
  `/v1/organizations/${organizationId}/properties/${propertyId}/media`

export const propertyMediaApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    listPropertyMedia: builder.query({
      query: mediaPath,
      providesTags: (result, error, args) => [
        { type: 'PropertyMedia', id: `${args.organizationId}:${args.propertyId}` },
      ],
    }),
    createMediaUploadIntent: builder.mutation({
      async queryFn({ organizationId, propertyId, ...body }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `${mediaPath({ organizationId, propertyId })}/uploads/signature`,
            { method: 'POST', body: JSON.stringify(body) },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
    }),
    completeMediaUpload: builder.mutation({
      async queryFn({ organizationId, propertyId, mediaId, ...body }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `${mediaPath({ organizationId, propertyId })}/${mediaId}/complete`,
            { method: 'POST', body: JSON.stringify(body) },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: (result, error, args) => [
        { type: 'PropertyMedia', id: `${args.organizationId}:${args.propertyId}` },
      ],
    }),
    selectPrimaryMedia: builder.mutation({
      async queryFn({ organizationId, propertyId, mediaId }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `${mediaPath({ organizationId, propertyId })}/${mediaId}/primary`,
            { method: 'POST' },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: (result, error, args) => [
        { type: 'PropertyMedia', id: `${args.organizationId}:${args.propertyId}` },
      ],
    }),
    reorderPropertyMedia: builder.mutation({
      async queryFn({ organizationId, propertyId, mediaIds }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `${mediaPath({ organizationId, propertyId })}/order`,
            { method: 'PUT', body: JSON.stringify({ mediaIds }) },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: (result, error, args) => [
        { type: 'PropertyMedia', id: `${args.organizationId}:${args.propertyId}` },
      ],
    }),
    deletePropertyMedia: builder.mutation({
      async queryFn({ organizationId, propertyId, mediaId }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `${mediaPath({ organizationId, propertyId })}/${mediaId}`,
            { method: 'DELETE' },
            dispatch,
          )
          return { data }
        } catch (error) {
          return { error: { status: error.status, data: error.data } }
        }
      },
      invalidatesTags: (result, error, args) => [
        { type: 'PropertyMedia', id: `${args.organizationId}:${args.propertyId}` },
      ],
    }),
  }),
})

export const {
  useListPropertyMediaQuery,
  useCreateMediaUploadIntentMutation,
  useCompleteMediaUploadMutation,
  useSelectPrimaryMediaMutation,
  useReorderPropertyMediaMutation,
  useDeletePropertyMediaMutation,
} = propertyMediaApi
