package com.rahul.HostelOs.enquiry.dto;

import com.rahul.HostelOs.enquiry.Enquiry;
import com.rahul.HostelOs.enquiry.EnquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Enquiry owned by the authenticated customer.")
public record CustomerEnquiryResponse(
		Long id,
		Long listingId,
		String listingTitle,
		String organizationName,
		String message,
		EnquiryStatus status,
		long version,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static CustomerEnquiryResponse from(Enquiry enquiry) {
		return new CustomerEnquiryResponse(
				enquiry.getId(),
				enquiry.getListing().getId(),
				enquiry.getListing().getTitle(),
				enquiry.getOrganization().getName(),
				enquiry.getMessage(),
				enquiry.getStatus(),
				enquiry.getVersion(),
				enquiry.getCreatedAt(),
				enquiry.getUpdatedAt());
	}
}
