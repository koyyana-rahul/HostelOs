import { baseApi } from "../../../services/api/baseApi.js";
import { csrfRequest } from "../../../services/api/csrfRequest.js";

export const propertyApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    listProperties: builder.query({
      query: ({ organizationId, status }) => {
        const params = new URLSearchParams();
        if (status) params.set("status", status);
        const query = params.toString() ? `?${params.toString()}` : "";
        return `/v1/organizations/${organizationId}/properties${query}`;
      },
      providesTags: (result, error, { organizationId }) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: "Property", id })),
              { type: "Property", id: `ORG:${organizationId}` },
            ]
          : [{ type: "Property", id: `ORG:${organizationId}` }],
    }),
    getProperty: builder.query({
      query: ({ organizationId, propertyId }) =>
        `/v1/organizations/${organizationId}/properties/${propertyId}`,
      providesTags: (result, error, { propertyId }) => [
        { type: "Property", id: propertyId },
      ],
    }),
    createProperty: builder.mutation({
      async queryFn({ organizationId, ...body }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `/v1/organizations/${organizationId}/properties`,
            { method: "POST", body: JSON.stringify(body) },
            dispatch,
          );
          return { data };
        } catch (error) {
          return { error: { status: error.status, data: error.data } };
        }
      },
      invalidatesTags: (result, error, { organizationId }) => [
        { type: "Property", id: `ORG:${organizationId}` },
        { type: "Dashboard", id: organizationId },
      ],
    }),
    updateProperty: builder.mutation({
      async queryFn({ organizationId, propertyId, ...body }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `/v1/organizations/${organizationId}/properties/${propertyId}`,
            { method: "PUT", body: JSON.stringify(body) },
            dispatch,
          );
          return { data };
        } catch (error) {
          return { error: { status: error.status, data: error.data } };
        }
      },
      invalidatesTags: (result, error, { organizationId, propertyId }) => [
        { type: "Property", id: propertyId },
        { type: "Property", id: `ORG:${organizationId}` },
        { type: "Dashboard", id: organizationId },
      ],
    }),
    changePropertyStatus: builder.mutation({
      async queryFn({ organizationId, propertyId, status }, { dispatch }) {
        try {
          const data = await csrfRequest(
            `/v1/organizations/${organizationId}/properties/${propertyId}/status`,
            { method: "PATCH", body: JSON.stringify({ status }) },
            dispatch,
          );
          return { data };
        } catch (error) {
          return { error: { status: error.status, data: error.data } };
        }
      },
      invalidatesTags: (result, error, { organizationId, propertyId }) => [
        { type: "Property", id: propertyId },
        { type: "Property", id: `ORG:${organizationId}` },
        { type: "Dashboard", id: organizationId },
      ],
    }),
  }),
});

export const {
  useListPropertiesQuery,
  useGetPropertyQuery,
  useCreatePropertyMutation,
  useUpdatePropertyMutation,
  useChangePropertyStatusMutation,
} = propertyApi;
