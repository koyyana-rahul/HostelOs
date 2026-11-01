package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException;
import com.rahul.HostelOs.organization.exception.OrganizationNotFoundException;
import java.util.Objects;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrganizationAccessPolicy {

	private final OrganizationRepository organizationRepository;

	public OrganizationAccessPolicy(
			OrganizationRepository organizationRepository) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository, "OrganizationRepository must not be null");
	}

	public Long currentUserId() {
		return currentPrincipal().getUserId();
	}

	public HostelOsPrincipal currentPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
			throw new OrganizationAccessDeniedException("Authentication is required");
		}
		if (!(authentication.getPrincipal() instanceof HostelOsPrincipal principal) || principal.getUserId() == null) {
			throw new OrganizationAccessDeniedException("Authentication is required");
		}
		return principal;
	}

	public Organization requireActiveOrganization(Long organizationId) {
		if (organizationId == null || organizationId <= 0L) {
			throw new OrganizationNotFoundException("Organization not found: " + organizationId);
		}
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + organizationId));
		if (organization.getStatus() != OrganizationStatus.ACTIVE) {
			throw new OrganizationAccessDeniedException("Organization is not active: " + organizationId);
		}
		return organization;
	}

	public Organization requireOwnerOrAdmin(Long organizationId, Long userId) {
		Organization organization = requireActiveOrganization(organizationId);
		if (!organization.isOwnedBy(userId)) {
			throw new OrganizationAccessDeniedException("Access is denied for organization: " + organizationId);
		}
		return organization;
	}

	public Organization requireMemberAccess(Long organizationId, Long userId) {
		Organization organization = requireActiveOrganization(organizationId);
		if (!organization.isOwnedBy(userId)) {
			throw new OrganizationAccessDeniedException("Access is denied for organization: " + organizationId);
		}
		return organization;
	}
}
