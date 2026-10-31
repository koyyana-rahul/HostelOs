package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.organization.dto.OrganizationDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/dashboard")
@Tag(name = "Organization Dashboard", description = "Owner workspace metrics")
public class OrganizationDashboardController {

	private final OrganizationDashboardService dashboardService;

	public OrganizationDashboardController(OrganizationDashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
	@Operation(summary = "Get organization dashboard metrics")
	public OrganizationDashboardResponse getDashboard(@PathVariable Long organizationId) {
		return dashboardService.getDashboard(organizationId);
	}
}
