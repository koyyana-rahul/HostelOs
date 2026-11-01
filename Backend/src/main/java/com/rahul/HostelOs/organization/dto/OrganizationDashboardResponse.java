package com.rahul.HostelOs.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aggregated owner dashboard metrics.")
public record OrganizationDashboardResponse(
		Long organizationId,
		String organizationName,
		long propertyCount,
		long activePropertyCount,
		long inactivePropertyCount,
		long archivedPropertyCount,
		long listingCount,
		long draftListingCount,
		long publishedListingCount,
		long archivedListingCount,
		long enquiryCount,
		long newEnquiryCount,
		long readEnquiryCount,
		long respondedEnquiryCount,
		long closedEnquiryCount) {
}
