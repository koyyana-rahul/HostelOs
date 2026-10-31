package com.rahul.HostelOs.organization.dto;

import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.organization.OrganizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Organization details response.")
public record OrganizationResponse(
		@Schema(description = "Unique organization identifier.", example = "1")
		Long id,
		@Schema(description = "Organization name.", example = "Sunrise Hostel Group")
		String name,
		@Schema(description = "Canonical organization slug.", example = "sunrise-hostel")
		String slug,
		@Schema(description = "Organization operational status.", example = "ACTIVE")
		OrganizationStatus status,
		@Schema(description = "Timestamp when the organization was created.")
		LocalDateTime createdAt,
		@Schema(description = "Timestamp when the organization was last updated.")
		LocalDateTime updatedAt) {

	public static OrganizationResponse from(Organization organization) {
		return new OrganizationResponse(
				organization.getId(),
				organization.getName(),
				organization.getSlug(),
				organization.getStatus(),
				organization.getCreatedAt(),
				organization.getUpdatedAt());
	}
}
