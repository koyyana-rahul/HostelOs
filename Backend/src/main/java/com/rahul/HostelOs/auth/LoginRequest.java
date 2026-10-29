package com.rahul.HostelOs.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credentials for creating an authenticated session.")
public record LoginRequest(
		@NotBlank
		@Size(max = 254)
		@Schema(description = "Normalized login identifier.", example = "platform-admin@example.com")
		String loginIdentifier,
		@NotBlank
		@Size(max = 200)
		@Schema(description = "User password.", accessMode = Schema.AccessMode.WRITE_ONLY)
		String password) {
}
