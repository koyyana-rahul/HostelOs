package com.rahul.HostelOs.auth;

import com.rahul.HostelOs.identity.HostelOsPrincipal;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Safe identity information for the authenticated session.")
public record AuthUserResponse(
		@Schema(description = "Server-side user identifier.", example = "1")
		Long id,
		@Schema(description = "Normalized login identifier.", example = "admin@example.com")
		String loginIdentifier,
		@Schema(description = "Human-readable display name.", example = "HostelOS Administrator")
		String displayName,
		@Schema(description = "Server-assigned roles.", example = "[\"PLATFORM_ADMIN\"]")
		List<String> roles) {

	public static AuthUserResponse from(HostelOsPrincipal principal) {
		return new AuthUserResponse(
				principal.getUserId(),
				principal.getUsername(),
				principal.getDisplayName(),
				principal.getRoles());
	}
}
