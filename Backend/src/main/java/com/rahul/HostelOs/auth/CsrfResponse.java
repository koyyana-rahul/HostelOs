package com.rahul.HostelOs.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "CSRF token bootstrap data for the same-origin SPA.")
public record CsrfResponse(
		@Schema(description = "Header name expected on state-changing requests.", example = "X-XSRF-TOKEN")
		String headerName,
		@Schema(description = "CSRF token value.")
		String token) {
}
