package com.rahul.HostelOs.platform;

import com.rahul.HostelOs.platform.dto.PlatformStatsResponse;
import com.rahul.HostelOs.platform.dto.PlatformUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
@Tag(name = "Platform Administration", description = "Platform-level administration")
@SecurityRequirement(name = "sessionAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformController {

	private final PlatformService platformService;

	public PlatformController(PlatformService platformService) {
		this.platformService = platformService;
	}

	@Operation(summary = "Get platform statistics")
	@GetMapping("/stats")
	public PlatformStatsResponse getStats() {
		return platformService.getStats();
	}

	@Operation(summary = "List platform users")
	@GetMapping("/users")
	public Page<PlatformUserResponse> listUsers(
			@RequestParam(value = "search", required = false) String search,
			@PageableDefault(size = 20) Pageable pageable) {
		return platformService.listUsers(search, pageable);
	}

	@Operation(summary = "Get platform user details")
	@GetMapping("/users/{userId}")
	public PlatformUserResponse getUser(@PathVariable Long userId) {
		return platformService.getUser(userId);
	}

	@Operation(summary = "Suspend an organization")
	@PostMapping("/organizations/{organizationId}/suspend")
	@ResponseStatus(HttpStatus.OK)
	public void suspendOrganization(@PathVariable Long organizationId) {
		platformService.suspendOrganization(organizationId);
	}

	@Operation(summary = "Reactivate an organization")
	@PostMapping("/organizations/{organizationId}/reactivate")
	@ResponseStatus(HttpStatus.OK)
	public void reactivateOrganization(@PathVariable Long organizationId) {
		platformService.reactivateOrganization(organizationId);
	}
}
