package com.rahul.HostelOs.system;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Non-sensitive system metadata for the HostelOS API.")
public record SystemInfoResponse(
		@Schema(description = "Application name.", example = "HostelOS")
		String applicationName,
		@Schema(description = "API version.", example = "v1")
		String apiVersion,
		@Schema(
				description = "Status of the system-information endpoint; not a dependency health report.",
				example = "AVAILABLE")
		String status) {
}
