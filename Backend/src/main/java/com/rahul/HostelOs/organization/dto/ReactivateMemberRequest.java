package com.rahul.HostelOs.organization.dto;

import com.rahul.HostelOs.organization.OrgRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for reactivating a suspended or revoked membership.")
public record ReactivateMemberRequest(
		@NotNull(message = "Org role for reactivation must not be null")
		@Schema(description = "Role to assign upon reactivation.", example = "ORG_STAFF")
		OrgRole orgRole) {
}
