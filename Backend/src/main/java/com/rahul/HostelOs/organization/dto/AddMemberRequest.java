package com.rahul.HostelOs.organization.dto;

import com.rahul.HostelOs.organization.OrgRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for adding a user to an organization.")
public record AddMemberRequest(
		@NotBlank(message = "User login identifier must not be blank")
		@Size(min = 3, max = 254, message = "User login identifier must be between 3 and 254 characters")
		@Schema(description = "Login identifier of the user to add.", example = "alice@example.com")
		String userLoginIdentifier,

		@NotNull(message = "Org role must not be null")
		@Schema(description = "Initial role for the new member.", example = "ORG_STAFF")
		OrgRole orgRole) {
}
