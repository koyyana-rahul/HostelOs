package com.rahul.HostelOs.organization.dto;

import com.rahul.HostelOs.organization.MembershipStatus;
import com.rahul.HostelOs.organization.OrgRole;
import com.rahul.HostelOs.organization.OrganizationMembership;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Organization membership response.")
public record MembershipResponse(
		@Schema(description = "Membership identifier.", example = "1")
		Long id,
		@Schema(description = "Organization identifier.", example = "1")
		Long organizationId,
		@Schema(description = "Organization name.", example = "Sunrise Hostel Group")
		String organizationName,
		@Schema(description = "User identifier.", example = "10")
		Long userId,
		@Schema(description = "User display name.", example = "Alice Smith")
		String userDisplayName,
		@Schema(description = "User login identifier.", example = "alice@example.com")
		String userLoginIdentifier,
		@Schema(description = "Organization role.", example = "ORG_STAFF")
		OrgRole orgRole,
		@Schema(description = "Membership operational status.", example = "ACTIVE")
		MembershipStatus status,
		@Schema(description = "Timestamp when the membership was created.")
		LocalDateTime createdAt,
		@Schema(description = "Timestamp when the membership was last updated.")
		LocalDateTime updatedAt) {

	public static MembershipResponse from(OrganizationMembership m) {
		return new MembershipResponse(
				m.getId(),
				m.getOrganization().getId(),
				m.getOrganization().getName(),
				m.getUser().getId(),
				m.getUser().getDisplayName(),
				m.getUser().getLoginIdentifier(),
				m.getOrgRole(),
				m.getStatus(),
				m.getCreatedAt(),
				m.getUpdatedAt());
	}
}
