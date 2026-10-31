package com.rahul.HostelOs.enquiry.dto;

import com.rahul.HostelOs.enquiry.Enquiry;
import com.rahul.HostelOs.enquiry.EnquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Organization enquiry details.")
public record EnquiryResponse(
		Long id,
		Long organizationId,
		Long listingId,
		String listingTitle,
		Long customerUserId,
		String customerName,
		String customerEmail,
		String customerPhone,
		String message,
		EnquiryStatus status,
		long version,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static EnquiryResponse from(Enquiry enquiry) {
		return new EnquiryResponse(
				enquiry.getId(),
				enquiry.getOrganization().getId(),
				enquiry.getListing().getId(),
				enquiry.getListing().getTitle(),
				enquiry.getCustomerUserId(),
				enquiry.getCustomerName(),
				enquiry.getCustomerEmail(),
				enquiry.getCustomerPhone(),
				enquiry.getMessage(),
				enquiry.getStatus(),
				enquiry.getVersion(),
				enquiry.getCreatedAt(),
				enquiry.getUpdatedAt());
	}
}
