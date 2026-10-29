package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.organization.dto.CreateOrganizationRequest;
import com.rahul.HostelOs.organization.dto.OrganizationResponse;
import com.rahul.HostelOs.organization.exception.DuplicateOrganizationSlugException;
import java.util.Locale;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationProvisioningService {

	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;
	private final UserAccountRepository userAccountRepository;

	public OrganizationProvisioningService(
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository membershipRepository,
			UserAccountRepository userAccountRepository) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository, "OrganizationRepository must not be null");
		this.membershipRepository = Objects.requireNonNull(membershipRepository, "OrganizationMembershipRepository must not be null");
		this.userAccountRepository = Objects.requireNonNull(userAccountRepository, "UserAccountRepository must not be null");
	}

	@Transactional
	public OrganizationResponse provisionOrganization(CreateOrganizationRequest request, Long creatorUserId) {
		if (request == null) {
			throw new IllegalArgumentException("CreateOrganizationRequest must not be null");
		}
		return provisionOrganization(request.name(), request.slug(), creatorUserId);
	}

	@Transactional
	public OrganizationResponse provisionOrganization(String rawName, String rawSlug, Long creatorUserId) {
		if (creatorUserId == null || creatorUserId <= 0L) {
			throw new IllegalArgumentException("Creator user ID must be a positive non-null value");
		}

		if (rawName == null || rawName.trim().isBlank()) {
			throw new IllegalArgumentException("Organization name must not be blank");
		}
		String name = rawName.trim();
		if (name.length() > 120) {
			throw new IllegalArgumentException("Organization name must not exceed 120 characters");
		}

		String slug = OrganizationSlug.normalize(rawSlug);

		if (organizationRepository.existsBySlug(slug)) {
			throw new DuplicateOrganizationSlugException("Organization slug already exists: " + slug);
		}

		UserAccount creator = userAccountRepository.findById(creatorUserId)
				.orElseThrow(() -> new IllegalArgumentException("Creator user account not found: " + creatorUserId));

		if (creator.getStatus() != AccountStatus.ACTIVE) {
			throw new IllegalStateException("Creator account is not active");
		}

		try {
			Organization organization = new Organization(name, slug, OrganizationStatus.ACTIVE);
			Organization savedOrg = organizationRepository.save(organization);

			OrganizationMembership membership = new OrganizationMembership(
					savedOrg, creator, OrgRole.ORG_ADMIN, MembershipStatus.ACTIVE);
			membershipRepository.save(membership);

			membershipRepository.flush();

			return OrganizationResponse.from(savedOrg);
		} catch (DataIntegrityViolationException ex) {
			if (isSlugConstraintViolation(ex)) {
				throw new DuplicateOrganizationSlugException("Organization slug already exists: " + slug);
			}
			throw ex;
		}
	}

	private boolean isSlugConstraintViolation(DataIntegrityViolationException ex) {
		Throwable current = ex;
		while (current != null) {
			if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
				String constraintName = cve.getConstraintName();
				if (constraintName != null && "uk_organizations_slug".equalsIgnoreCase(constraintName.trim())) {
					return true;
				}
			}
			String message = current.getMessage();
			if (message != null) {
				String lower = message.toLowerCase(Locale.ROOT);
				if (lower.contains("uk_organizations_slug")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}
}
