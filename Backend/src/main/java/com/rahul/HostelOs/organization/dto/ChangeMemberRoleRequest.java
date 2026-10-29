package com.rahul.HostelOs.organization.dto;

import com.rahul.HostelOs.organization.OrgRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for changing a member's role.")
public record ChangeMemberRoleRequest(
		@NotNull(message = "New org role must not be null")
		@Schema(description = "New role for the member.", example = "ORG_ADMIN")
		OrgRole newRole) {
}
