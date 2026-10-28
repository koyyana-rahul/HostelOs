package com.rahul.HostelOs.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "Non-sensitive system metadata")
public class SystemInfoController {

	private static final String APPLICATION_NAME = "HostelOS";
	private static final String API_VERSION = "v1";
	private static final String STATUS = "AVAILABLE";

	@Operation(
			summary = "Get system information",
			description = "Returns non-sensitive application metadata. The status field describes this endpoint's availability, not the health of every dependency.")
	@ApiResponse(responseCode = "200", description = "System information returned")
	@GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public SystemInfoResponse getInfo() {
		return new SystemInfoResponse(APPLICATION_NAME, API_VERSION, STATUS);
	}
}
