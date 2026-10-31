package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.organization.dto.OrganizationResponse;
import com.rahul.HostelOs.organization.dto.UpdateOrganizationRequest;
import com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException;
import com.rahul.HostelOs.organization.exception.OrganizationNotFoundException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrganizationOwnershipService {

	private final OrganizationRepository organizationRepository;

	public OrganizationOwnershipService(OrganizationRepository organizationRepository) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository, "OrganizationRepository must not be null");
	}

	public List<OrganizationResponse> listOwnedOrganizations(Long userId) {
		if (userId == null) {
			return List.of();
		}
		return organizationRepository.findByOwnerUserIdOrderByCreatedAtAsc(userId).stream()
				.map(OrganizationResponse::from)
				.toList();
	}

	public OrganizationResponse getOwnedOrganization(Long userId) {
		if (userId == null) {
			throw new OrganizationAccessDeniedException("Authentication is required");
		}
		List<Organization> owned = organizationRepository
				.findByOwnerUserIdAndStatusOrderByCreatedAtAsc(userId, OrganizationStatus.ACTIVE);
		if (owned.isEmpty()) {
			return null;
		}
		return OrganizationResponse.from(owned.get(0));
	}

	@Transactional
	public OrganizationResponse updateOrganization(Long organizationId, UpdateOrganizationRequest request, Long userId) {
		if (request == null) {
			throw new IllegalArgumentException("UpdateOrganizationRequest must not be null");
		}
		Organization organization = organizationRepository.findByIdWithLock(organizationId)
				.orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + organizationId));
		if (!organization.isOwnedBy(userId)) {
			throw new OrganizationAccessDeniedException("Access is denied for organization: " + organizationId);
		}
		if (organization.getStatus() != OrganizationStatus.ACTIVE) {
			throw new OrganizationAccessDeniedException("Organization is not active: " + organizationId);
		}
		organization.updateName(request.name());
		return OrganizationResponse.from(organizationRepository.save(organization));
	}
}
