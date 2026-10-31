package com.rahul.HostelOs.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for creating a new organization.")
public record CreateOrganizationRequest(
		@NotBlank(message = "Organization name must not be blank")
		@Size(max = 120, message = "Organization name must not exceed 120 characters")
		@Schema(description = "Human-readable organization name.", example = "Sunrise Hostel Group")
		String name,

		@NotBlank(message = "Organization slug must not be blank")
		@Size(min = 3, max = 60, message = "Organization slug must be between 3 and 60 characters")
		@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Organization slug must consist of lowercase alphanumeric characters and hyphens")
		@Schema(description = "URL-friendly canonical organization slug.", example = "sunrise-hostel")
		String slug) {
}
