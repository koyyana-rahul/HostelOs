package com.rahul.HostelOs.inventory;

import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.inventory.dto.ChangePropertyStatusRequest;
import com.rahul.HostelOs.inventory.dto.CreatePropertyRequest;
import com.rahul.HostelOs.inventory.dto.PropertyResponse;
import com.rahul.HostelOs.inventory.dto.UpdatePropertyRequest;
import com.rahul.HostelOs.inventory.exception.DuplicatePropertyCodeException;
import com.rahul.HostelOs.inventory.exception.InvalidPropertyStatusTransitionException;
import com.rahul.HostelOs.inventory.exception.PropertyNotFoundException;
import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.organization.OrganizationRepository;
import com.rahul.HostelOs.organization.OrganizationStatus;
import com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException;
import com.rahul.HostelOs.organization.exception.OrganizationNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PropertyManagementService {

	private final PropertyRepository propertyRepository;
	private final OrganizationRepository organizationRepository;

public PropertyManagementService(
		PropertyRepository propertyRepository,
		OrganizationRepository organizationRepository) {
	this.propertyRepository = Objects.requireNonNull(propertyRepository, "PropertyRepository must not be null");
	this.organizationRepository = Objects.requireNonNull(organizationRepository, "OrganizationRepository must not be null");
}

	public PropertyResponse createProperty(Long organizationId, CreatePropertyRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("CreatePropertyRequest must not be null");
		}

		Long callerUserId = resolveActingUserId();
		Organization org = lockAndAuthorizeAdmin(organizationId, callerUserId);

		String normalizedCode = PropertyCode.normalize(request.code());

		if (propertyRepository.existsByOrganizationIdAndCode(organizationId, normalizedCode)) {
			throw new DuplicatePropertyCodeException(normalizedCode, organizationId);
		}

		Property property = new Property(
				org,
				request.name(),
				request.code(),
				PropertyStatus.ACTIVE,
				request.addressLine1(),
				request.addressLine2(),
				request.city(),
				request.state(),
				request.postalCode(),
				request.country(),
				request.timezone(),
				request.latitude(),
				request.longitude(),
				request.placeId(),
				request.locationProvider());

		try {
			Property saved = propertyRepository.save(property);
			propertyRepository.flush();
			return PropertyResponse.from(saved);
		} catch (DataIntegrityViolationException ex) {
			if (isDuplicateCodeConstraintViolation(ex)) {
				throw new DuplicatePropertyCodeException(property.getCode(), organizationId);
			}
			throw ex;
		}
	}

	public PropertyResponse updateProperty(Long organizationId, Long propertyId, UpdatePropertyRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("UpdatePropertyRequest must not be null");
		}
		if (propertyId == null || propertyId <= 0L) {
			throw new PropertyNotFoundException(propertyId, organizationId);
		}

		Long callerUserId = resolveActingUserId();
		lockAndAuthorizeAdmin(organizationId, callerUserId);

		Property property = propertyRepository.findByIdAndOrganizationIdWithLock(propertyId, organizationId)
				.orElseThrow(() -> new PropertyNotFoundException(propertyId, organizationId));

		if (property.getStatus() == PropertyStatus.ARCHIVED) {
			throw new InvalidPropertyStatusTransitionException("Cannot update details of an archived property");
		}

property.updateDetails(
			request.name(),
			request.addressLine1(),
			request.addressLine2(),
			request.city(),
			request.state(),
			request.postalCode(),
			request.country(),
			request.timezone(),
			request.latitude(),
			request.longitude());

		Property saved = propertyRepository.save(property);
		propertyRepository.flush();
		return PropertyResponse.from(saved);
	}

	public PropertyResponse changeStatus(Long organizationId, Long propertyId, ChangePropertyStatusRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("ChangePropertyStatusRequest must not be null");
		}
		if (request.status() == null) {
			throw new IllegalArgumentException("Property status must not be null");
		}
		if (propertyId == null || propertyId <= 0L) {
			throw new PropertyNotFoundException(propertyId, organizationId);
		}

		Long callerUserId = resolveActingUserId();
		lockAndAuthorizeAdmin(organizationId, callerUserId);

		Property property = propertyRepository.findByIdAndOrganizationIdWithLock(propertyId, organizationId)
				.orElseThrow(() -> new PropertyNotFoundException(propertyId, organizationId));

		PropertyStatus targetStatus = request.status();
		if (property.getStatus() == PropertyStatus.ARCHIVED && targetStatus != PropertyStatus.ARCHIVED) {
			throw new InvalidPropertyStatusTransitionException(
					"Cannot transition property status from ARCHIVED to " + targetStatus);
		}

		if (targetStatus == PropertyStatus.ACTIVE) {
			property.activate();
		} else if (targetStatus == PropertyStatus.INACTIVE) {
			property.suspend();
		} else if (targetStatus == PropertyStatus.ARCHIVED) {
			property.archive();
		}

		Property saved = propertyRepository.save(property);
		propertyRepository.flush();
		return PropertyResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public PropertyResponse getProperty(Long organizationId, Long propertyId) {
		if (propertyId == null || propertyId <= 0L) {
			throw new PropertyNotFoundException(propertyId, organizationId);
		}

		Long callerUserId = resolveActingUserId();
		authorizeReadAccess(organizationId, callerUserId);

		Property property = propertyRepository.findByOrganizationIdAndId(organizationId, propertyId)
				.orElseThrow(() -> new PropertyNotFoundException(propertyId, organizationId));

		return PropertyResponse.from(property);
	}

	@Transactional(readOnly = true)
	public List<PropertyResponse> listProperties(Long organizationId, PropertyStatus status) {
		Long callerUserId = resolveActingUserId();
		authorizeReadAccess(organizationId, callerUserId);

		List<Property> properties = (status != null)
				? propertyRepository.findByOrganizationIdAndStatus(organizationId, status)
				: propertyRepository.findByOrganizationId(organizationId);

		return properties.stream().map(PropertyResponse::from).toList();
	}

	private Organization lockAndAuthorizeAdmin(Long organizationId, Long callerUserId) {
		if (organizationId == null || organizationId <= 0L) {
			throw new OrganizationNotFoundException("Organization not found: " + organizationId);
		}

		Organization org = organizationRepository.findByIdWithLock(organizationId)
				.orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + organizationId));

		if (org.getStatus() != OrganizationStatus.ACTIVE) {
			throw new OrganizationAccessDeniedException("Organization is not active: " + organizationId);
		}

		if (org.isOwnedBy(callerUserId)) {
			return org;
		}

		throw new OrganizationAccessDeniedException("Only the organization owner has access");
	}

	private Organization authorizeReadAccess(Long organizationId, Long callerUserId) {
		if (organizationId == null || organizationId <= 0L) {
			throw new OrganizationNotFoundException("Organization not found: " + organizationId);
		}

		Organization org = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + organizationId));

		if (org.getStatus() != OrganizationStatus.ACTIVE) {
			throw new OrganizationAccessDeniedException("Organization is not active: " + organizationId);
		}

		if (org.isOwnedBy(callerUserId)) {
			return org;
		}

		throw new OrganizationAccessDeniedException("Only the organization owner has read access");
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

	private boolean isDuplicateCodeConstraintViolation(DataIntegrityViolationException ex) {
		Throwable current = ex;
		while (current != null) {
			if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
				String constraintName = cve.getConstraintName();
				if (constraintName != null && "uk_properties_org_code".equalsIgnoreCase(constraintName.trim())) {
					return true;
				}
			}
			String message = current.getMessage();
			if (message != null) {
				String lower = message.toLowerCase(Locale.ROOT);
				if (lower.contains("uk_properties_org_code")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}
}
