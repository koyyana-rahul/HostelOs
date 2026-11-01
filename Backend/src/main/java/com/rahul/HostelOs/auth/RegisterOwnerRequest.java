package com.rahul.HostelOs.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Public registration request for a new organization owner account.")
public record RegisterOwnerRequest(
		@NotBlank
		@Size(max = 254)
		@Schema(description = "Login identifier, typically an email address.")
		String loginIdentifier,
		@NotBlank
		@Size(max = 120)
		@Schema(description = "Human-readable display name.")
		String displayName,
		@NotBlank
		@Size(max = 200)
		@Schema(description = "User password.", accessMode = Schema.AccessMode.WRITE_ONLY)
		String password) {
}
