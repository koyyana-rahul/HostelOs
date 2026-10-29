package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.identity.LoginIdentifier;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.organization.dto.AddMemberRequest;
import com.rahul.HostelOs.organization.dto.ChangeMemberRoleRequest;
import com.rahul.HostelOs.organization.dto.MembershipResponse;
import com.rahul.HostelOs.organization.dto.ReactivateMemberRequest;
import com.rahul.HostelOs.organization.exception.InactiveUserAccountException;
import com.rahul.HostelOs.organization.exception.InvalidMembershipTransitionException;
import com.rahul.HostelOs.organization.exception.LastAdminProtectionException;
import com.rahul.HostelOs.organization.exception.MembershipAlreadyExistsException;
import com.rahul.HostelOs.organization.exception.MembershipNotFoundException;
import com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException;
import com.rahul.HostelOs.organization.exception.OrganizationNotFoundException;
import com.rahul.HostelOs.organization.exception.UserNotFoundException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MembershipManagementService {

	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;
	private final UserAccountRepository userAccountRepository;

	public MembershipManagementService(
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository membershipRepository,
			UserAccountRepository userAccountRepository) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository, "OrganizationRepository must not be null");
		this.membershipRepository = Objects.requireNonNull(membershipRepository, "OrganizationMembershipRepository must not be null");
		this.userAccountRepository = Objects.requireNonNull(userAccountRepository, "UserAccountRepository must not be null");
	}

	public MembershipResponse addMember(Long organizationId, AddMemberRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("AddMemberRequest must not be null");
		}
		if (request.orgRole() == null) {
			throw new IllegalArgumentException("Org role must not be null");
		}
		if (request.userLoginIdentifier() == null || request.userLoginIdentifier().trim().isBlank()) {
			throw new IllegalArgumentException("User login identifier must not be blank");
		}

		Long callerUserId = resolveActingUserId();
		Organization org = lockAndAuthorizeAdmin(organizationId, callerUserId);

		String normalizedLogin = LoginIdentifier.normalize(request.userLoginIdentifier());
		UserAccount targetUser = userAccountRepository.findByLoginIdentifier(normalizedLogin)
				.orElseThrow(() -> new UserNotFoundException("User not found: " + normalizedLogin));

		if (targetUser.getStatus() != AccountStatus.ACTIVE) {
			throw new InactiveUserAccountException("Cannot add inactive user: " + normalizedLogin);
		}

		Optional<OrganizationMembership> existingOpt = membershipRepository
				.findByOrganizationIdAndUserId(organizationId, targetUser.getId());
		if (existingOpt.isPresent()) {
			throw new MembershipAlreadyExistsException(existingOpt.get().getStatus());
		}

		try {
			OrganizationMembership membership = new OrganizationMembership(
					org, targetUser, request.orgRole(), MembershipStatus.ACTIVE);
			OrganizationMembership saved = membershipRepository.save(membership);
			membershipRepository.flush();
			return MembershipResponse.from(saved);
		} catch (DataIntegrityViolationException ex) {
			if (isDuplicateMembershipConstraintViolation(ex)) {
				// Rollback-safe: do not query DB in corrupted session
				throw new MembershipAlreadyExistsException("User already has a membership in this organization: " + normalizedLogin);
			}
			throw ex;
		}
	}

	public MembershipResponse changeRole(Long organizationId, Long membershipId, ChangeMemberRoleRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("ChangeMemberRoleRequest must not be null");
		}
		if (request.newRole() == null) {
			throw new IllegalArgumentException("New org role must not be null");
		}

		Long callerUserId = resolveActingUserId();
		lockAndAuthorizeAdmin(organizationId, callerUserId);

		OrganizationMembership membership = loadMembershipForOrgWithLock(organizationId, membershipId);

		if (membership.getStatus() != MembershipStatus.ACTIVE) {
			throw new InvalidMembershipTransitionException("Cannot change role of non-active membership: " + membershipId);
		}

		if (membership.getOrgRole() == OrgRole.ORG_ADMIN && request.newRole() != OrgRole.ORG_ADMIN) {
			assertNotLastActiveAdmin(organizationId, membership);
		}

		membership.changeRole(request.newRole());
		OrganizationMembership saved = membershipRepository.save(membership);
		return MembershipResponse.from(saved);
	}

	public MembershipResponse suspendMembership(Long organizationId, Long membershipId) {
		Long callerUserId = resolveActingUserId();
		lockAndAuthorizeAdmin(organizationId, callerUserId);

		OrganizationMembership membership = loadMembershipForOrgWithLock(organizationId, membershipId);

		if (membership.getStatus() == MembershipStatus.REVOKED) {
			throw new InvalidMembershipTransitionException("Cannot suspend a revoked membership: " + membershipId);
		}

		if (membership.getStatus() == MembershipStatus.SUSPENDED) {
			return MembershipResponse.from(membership); // Idempotent
		}

		if (membership.getOrgRole() == OrgRole.ORG_ADMIN) {
			assertNotLastActiveAdmin(organizationId, membership);
		}

		membership.suspend();
		OrganizationMembership saved = membershipRepository.save(membership);
		return MembershipResponse.from(saved);
	}

	public MembershipResponse revokeMembership(Long organizationId, Long membershipId) {
		Long callerUserId = resolveActingUserId();
		lockAndAuthorizeAdmin(organizationId, callerUserId);

		OrganizationMembership membership = loadMembershipForOrgWithLock(organizationId, membershipId);

		if (membership.getStatus() == MembershipStatus.REVOKED) {
			return MembershipResponse.from(membership); // Idempotent
		}

		if (membership.getStatus() == MembershipStatus.ACTIVE && membership.getOrgRole() == OrgRole.ORG_ADMIN) {
			assertNotLastActiveAdmin(organizationId, membership);
		}

		membership.revoke();
		OrganizationMembership saved = membershipRepository.save(membership);
		return MembershipResponse.from(saved);
	}

	public MembershipResponse reactivateMembership(Long organizationId, Long membershipId, ReactivateMemberRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("ReactivateMemberRequest must not be null");
		}
		if (request.orgRole() == null) {
			throw new IllegalArgumentException("Org role for reactivation must not be null");
		}

		Long callerUserId = resolveActingUserId();
		lockAndAuthorizeAdmin(organizationId, callerUserId);

		OrganizationMembership membership = loadMembershipForOrgWithLock(organizationId, membershipId);

		if (membership.getStatus() == MembershipStatus.ACTIVE) {
			throw new InvalidMembershipTransitionException("Membership is already active: " + membershipId);
		}

		membership.reactivate(request.orgRole());
		OrganizationMembership saved = membershipRepository.save(membership);
		return MembershipResponse.from(saved);
	}

	private Long resolveActingUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
			throw new OrganizationAccessDeniedException("Unauthenticated caller");
		}

		if (!(authentication.getPrincipal() instanceof HostelOsPrincipal principal)) {
			throw new OrganizationAccessDeniedException("Invalid principal type");
		}

		Long userId = principal.getUserId();
		if (userId == null) {
			throw new OrganizationAccessDeniedException("Principal user ID is null");
		}
		return userId;
	}

	private Organization lockAndAuthorizeAdmin(Long organizationId, Long callerUserId) {
		if (organizationId == null || organizationId <= 0L) {
			throw new OrganizationNotFoundException("Organization not found: " + organizationId);
		}

		// 1. Acquire pessimistic lock on organization row (current read, blocks concurrent mutations)
		Organization org = organizationRepository.findByIdWithLock(organizationId)
				.orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + organizationId));

		// 2. Validate organization operational status
		if (org.getStatus() != OrganizationStatus.ACTIVE) {
			throw new OrganizationAccessDeniedException("Organization is not active: " + organizationId);
		}

		// 3. Evaluate caller membership under the lock
		OrganizationMembership callerMembership = membershipRepository
				.findByOrganizationIdAndUserId(organizationId, callerUserId)
				.orElseThrow(() -> new OrganizationAccessDeniedException("Caller has no membership in organization: " + organizationId));

		if (callerMembership.getStatus() != MembershipStatus.ACTIVE) {
			throw new OrganizationAccessDeniedException("Caller membership is not active in organization: " + organizationId);
		}

		if (callerMembership.getOrgRole() != OrgRole.ORG_ADMIN) {
			throw new OrganizationAccessDeniedException("Caller does not hold ORG_ADMIN role in organization: " + organizationId);
		}

		return org;
	}

	private OrganizationMembership loadMembershipForOrgWithLock(Long organizationId, Long membershipId) {
		if (membershipId == null || membershipId <= 0L) {
			throw new MembershipNotFoundException("Membership not found with id " + membershipId + " in organization " + organizationId);
		}

		return membershipRepository.findByIdAndOrganizationIdWithLock(membershipId, organizationId)
				.orElseThrow(() -> new MembershipNotFoundException(
						"Membership not found with id " + membershipId + " in organization " + organizationId));
	}

	private void assertNotLastActiveAdmin(Long organizationId, OrganizationMembership target) {
		if (target.getOrgRole() != OrgRole.ORG_ADMIN || target.getStatus() != MembershipStatus.ACTIVE) {
			return;
		}
		long activeAdminCount = membershipRepository.countActiveAdmins(organizationId);
		if (activeAdminCount <= 1L) {
			throw new LastAdminProtectionException(
					"Cannot remove or demote the last active administrator for organization: " + organizationId);
		}
	}

	private boolean isDuplicateMembershipConstraintViolation(DataIntegrityViolationException ex) {
		Throwable current = ex;
		while (current != null) {
			if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
				String constraintName = cve.getConstraintName();
				if (constraintName != null && "uk_org_memberships_org_user".equalsIgnoreCase(constraintName.trim())) {
					return true;
				}
			}
			String message = current.getMessage();
			if (message != null) {
				String lower = message.toLowerCase(Locale.ROOT);
				if (lower.contains("uk_org_memberships_org_user")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}
}
