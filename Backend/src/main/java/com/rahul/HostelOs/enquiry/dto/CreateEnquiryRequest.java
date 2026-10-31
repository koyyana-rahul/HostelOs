package com.rahul.HostelOs.enquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer enquiry submitted for a published listing.")
public record CreateEnquiryRequest(
		@Size(max = 120, message = "Name must not exceed 120 characters")
		String customerName,
		@Email(message = "Email must be valid")
		@Size(max = 254, message = "Email must not exceed 254 characters")
		String customerEmail,
		@Size(max = 40, message = "Phone must not exceed 40 characters")
		String customerPhone,
		@NotBlank(message = "Message must not be blank")
		@Size(max = 2000, message = "Message must not exceed 2000 characters")
		String message) {
}
