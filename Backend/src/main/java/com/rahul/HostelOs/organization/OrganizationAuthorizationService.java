package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.HostelOsPrincipal;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("orgAuth")
@Transactional(readOnly = true)
public class OrganizationAuthorizationService {

	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;

	public OrganizationAuthorizationService(
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository membershipRepository) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository, "OrganizationRepository must not be null");
		this.membershipRepository = Objects.requireNonNull(membershipRepository, "OrganizationMembershipRepository must not be null");
	}

	public boolean canAccessOrg(Long organizationId, OrgRole requiredRole) {
		if (organizationId == null || organizationId <= 0L) {
			return false;
		}
		if (requiredRole == null) {
			return false;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
			return false;
		}

		if (!(authentication.getPrincipal() instanceof HostelOsPrincipal principal)) {
			return false;
		}

		Long userId = principal.getUserId();
		if (userId == null) {
			return false;
		}

		Organization organization = organizationRepository.findById(organizationId).orElse(null);
		if (organization == null || organization.getStatus() != OrganizationStatus.ACTIVE) {
			return false;
		}

		Optional<OrganizationMembership> membershipOpt = membershipRepository
				.findByOrganizationIdAndUserIdAndStatus(organizationId, userId, MembershipStatus.ACTIVE);
		if (membershipOpt.isEmpty()) {
			return false;
		}

		OrganizationMembership membership = membershipOpt.get();
		OrgRole memberRole = membership.getOrgRole();
		if (memberRole == null) {
			return false;
		}

		if (requiredRole == OrgRole.ORG_ADMIN) {
			return memberRole == OrgRole.ORG_ADMIN;
		}
		if (requiredRole == OrgRole.ORG_STAFF) {
			return memberRole == OrgRole.ORG_ADMIN || memberRole == OrgRole.ORG_STAFF;
		}

		return false;
	}

	public boolean canAccessOrg(Long organizationId, String requiredRoleName) {
		if (requiredRoleName == null || requiredRoleName.isBlank()) {
			return false;
		}
		try {
			OrgRole role = OrgRole.valueOf(requiredRoleName.trim());
			return canAccessOrg(organizationId, role);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
