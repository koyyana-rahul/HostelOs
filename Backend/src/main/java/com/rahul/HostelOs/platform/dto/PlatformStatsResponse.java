package com.rahul.HostelOs.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlatformStatsResponse(
		long totalOrganizations,
		long activeOrganizations,
		long suspendedOrganizations,
		long totalUsers,
		long activeUsers,
		List<RecentOrganization> recentOrganizations,
		List<RecentUser> recentUsers) {

	public record RecentOrganization(Long id, String name, String slug, String status, LocalDateTime createdAt) {
	}

	public record RecentUser(Long id, String loginIdentifier, String displayName, String status, LocalDateTime createdAt) {
	}
}
