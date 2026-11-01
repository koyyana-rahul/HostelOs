package com.rahul.HostelOs.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Permitted customer profile updates.")
public record UpdateCustomerProfileRequest(
		@NotBlank(message = "Display name must not be blank")
		@Size(max = 120, message = "Display name must not exceed 120 characters")
		String displayName) {
}
