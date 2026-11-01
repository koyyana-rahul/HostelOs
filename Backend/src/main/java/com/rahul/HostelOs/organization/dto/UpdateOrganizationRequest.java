package com.rahul.HostelOs.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for updating organization settings.")
public record UpdateOrganizationRequest(
		@NotBlank(message = "Organization name must not be blank")
		@Size(max = 120, message = "Organization name must not exceed 120 characters")
		@Schema(description = "Human-readable organization name.", example = "Sunrise Hostel Group")
		String name) {
}
