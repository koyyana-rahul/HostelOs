package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.enquiry.EnquiryRepository;
import com.rahul.HostelOs.enquiry.EnquiryStatus;
import com.rahul.HostelOs.inventory.PropertyRepository;
import com.rahul.HostelOs.inventory.PropertyStatus;
import com.rahul.HostelOs.listing.ListingRepository;
import com.rahul.HostelOs.listing.ListingStatus;
import com.rahul.HostelOs.organization.dto.OrganizationDashboardResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrganizationDashboardService {

	private final OrganizationAccessPolicy accessPolicy;
	private final PropertyRepository propertyRepository;
	private final ListingRepository listingRepository;
	private final EnquiryRepository enquiryRepository;

	public OrganizationDashboardService(
			OrganizationAccessPolicy accessPolicy,
			PropertyRepository propertyRepository,
			ListingRepository listingRepository,
			EnquiryRepository enquiryRepository) {
		this.accessPolicy = Objects.requireNonNull(accessPolicy, "OrganizationAccessPolicy must not be null");
		this.propertyRepository = Objects.requireNonNull(propertyRepository, "PropertyRepository must not be null");
		this.listingRepository = Objects.requireNonNull(listingRepository, "ListingRepository must not be null");
		this.enquiryRepository = Objects.requireNonNull(enquiryRepository, "EnquiryRepository must not be null");
	}

	public OrganizationDashboardResponse getDashboard(Long organizationId) {
		Long userId = accessPolicy.currentUserId();
		Organization organization = accessPolicy.requireMemberAccess(organizationId, userId);
		return new OrganizationDashboardResponse(
				organization.getId(),
				organization.getName(),
				propertyRepository.countByOrganizationId(organizationId),
				propertyRepository.countByOrganizationIdAndStatus(organizationId, PropertyStatus.ACTIVE),
				propertyRepository.countByOrganizationIdAndStatus(organizationId, PropertyStatus.INACTIVE),
				propertyRepository.countByOrganizationIdAndStatus(organizationId, PropertyStatus.ARCHIVED),
				listingRepository.countByOrganizationId(organizationId),
				listingRepository.countByOrganizationIdAndStatus(organizationId, ListingStatus.DRAFT),
				listingRepository.countByOrganizationIdAndStatus(organizationId, ListingStatus.PUBLISHED),
				listingRepository.countByOrganizationIdAndStatus(organizationId, ListingStatus.ARCHIVED),
				enquiryRepository.countByOrganizationId(organizationId),
				enquiryRepository.countByOrganizationIdAndStatus(organizationId, EnquiryStatus.NEW),
				enquiryRepository.countByOrganizationIdAndStatus(organizationId, EnquiryStatus.READ),
				enquiryRepository.countByOrganizationIdAndStatus(organizationId, EnquiryStatus.RESPONDED),
				enquiryRepository.countByOrganizationIdAndStatus(organizationId, EnquiryStatus.CLOSED));
	}
}
