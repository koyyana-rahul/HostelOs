package com.rahul.HostelOs.platform;

import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.organization.OrganizationRepository;
import com.rahul.HostelOs.organization.OrganizationStatus;
import com.rahul.HostelOs.platform.dto.PlatformStatsResponse;
import com.rahul.HostelOs.platform.dto.PlatformUserResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformService {

	private final OrganizationRepository organizationRepository;
	private final UserAccountRepository userAccountRepository;

	public PlatformService(OrganizationRepository organizationRepository, UserAccountRepository userAccountRepository) {
		this.organizationRepository = organizationRepository;
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional(readOnly = true)
	public PlatformStatsResponse getStats() {
		long totalOrganizations = organizationRepository.count();
		long activeOrganizations = organizationRepository.countByStatus(OrganizationStatus.ACTIVE);
		long suspendedOrganizations = organizationRepository.countByStatus(OrganizationStatus.SUSPENDED);

		long totalUsers = userAccountRepository.count();
		long activeUsers = userAccountRepository.countByStatus(AccountStatus.ACTIVE);

		List<Organization> recentOrgs = organizationRepository.findTop5ByOrderByCreatedAtDesc();
		List<UserAccount> recentUsers = userAccountRepository.findTop5ByOrderByCreatedAtDesc();

		List<PlatformStatsResponse.RecentOrganization> recentOrgDtos = recentOrgs.stream()
				.map(o -> new PlatformStatsResponse.RecentOrganization(
						o.getId(), o.getName(), o.getSlug(), o.getStatus().name(), o.getCreatedAt()))
				.toList();

		List<PlatformStatsResponse.RecentUser> recentUserDtos = recentUsers.stream()
				.map(u -> new PlatformStatsResponse.RecentUser(
						u.getId(), u.getLoginIdentifier(), u.getDisplayName(), u.getStatus().name(), u.getCreatedAt()))
				.toList();

		return new PlatformStatsResponse(
				totalOrganizations,
				activeOrganizations,
				suspendedOrganizations,
				totalUsers,
				activeUsers,
				recentOrgDtos,
				recentUserDtos);
	}

	@Transactional(readOnly = true)
	public Page<PlatformUserResponse> listUsers(String search, Pageable pageable) {
		Page<UserAccount> page;
		if (search != null && !search.isBlank()) {
			String q = search.trim().toLowerCase();
			page = userAccountRepository.findByLoginIdentifierContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
					q, q, pageable);
		} else {
			page = userAccountRepository.findAll(pageable);
		}
		return page.map(this::toUserResponse);
	}

	@Transactional(readOnly = true)
	public PlatformUserResponse getUser(Long userId) {
		UserAccount user = userAccountRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
		return toUserResponse(user);
	}

	private PlatformUserResponse toUserResponse(UserAccount user) {
		List<String> roles = user.getRoles().stream()
				.map(r -> r.getName().name())
				.collect(Collectors.toList());
		return new PlatformUserResponse(user.getId(), user.getLoginIdentifier(), user.getDisplayName(),
				user.getStatus().name(), roles);
	}

	@Transactional
	public void suspendOrganization(Long organizationId) {
		Organization org = organizationRepository.findByIdWithLock(organizationId)
				.orElseThrow(() -> new com.rahul.HostelOs.organization.exception.OrganizationNotFoundException(
						"Organization not found: " + organizationId));
		if (org.getStatus() == OrganizationStatus.SUSPENDED) {
			return;
		}
		org.suspend();
		organizationRepository.save(org);
	}

	@Transactional
	public void reactivateOrganization(Long organizationId) {
		Organization org = organizationRepository.findByIdWithLock(organizationId)
				.orElseThrow(() -> new com.rahul.HostelOs.organization.exception.OrganizationNotFoundException(
						"Organization not found: " + organizationId));
		if (org.getStatus() == OrganizationStatus.ACTIVE) {
			return;
		}
		org.activate();
		organizationRepository.save(org);
	}
}
