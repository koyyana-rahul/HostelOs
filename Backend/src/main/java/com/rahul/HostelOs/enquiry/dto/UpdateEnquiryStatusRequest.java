package com.rahul.HostelOs.enquiry.dto;

import com.rahul.HostelOs.enquiry.EnquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for updating enquiry workflow status.")
public record UpdateEnquiryStatusRequest(
		@NotNull(message = "Enquiry status is required")
		EnquiryStatus status,
		@NotNull(message = "Version is required")
		Long version) {
}
