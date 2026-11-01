package com.rahul.HostelOs.enquiry;

import com.rahul.HostelOs.enquiry.dto.CreateEnquiryRequest;
import com.rahul.HostelOs.enquiry.dto.EnquiryResponse;
import com.rahul.HostelOs.enquiry.dto.UpdateEnquiryStatusRequest;
import com.rahul.HostelOs.enquiry.exception.EnquiryNotFoundException;
import com.rahul.HostelOs.enquiry.exception.EnquiryVersionConflictException;
import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.listing.Listing;
import com.rahul.HostelOs.listing.ListingRepository;
import com.rahul.HostelOs.listing.ListingStatus;
import com.rahul.HostelOs.listing.exception.ListingNotFoundException;
import com.rahul.HostelOs.organization.OrganizationAccessPolicy;
import com.rahul.HostelOs.organization.OrganizationStatus;
import com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnquiryService {

	private final EnquiryRepository enquiryRepository;
	private final ListingRepository listingRepository;
	private final OrganizationAccessPolicy accessPolicy;

	public EnquiryService(
			EnquiryRepository enquiryRepository,
			ListingRepository listingRepository,
			OrganizationAccessPolicy accessPolicy) {
		this.enquiryRepository = Objects.requireNonNull(enquiryRepository, "EnquiryRepository must not be null");
		this.listingRepository = Objects.requireNonNull(listingRepository, "ListingRepository must not be null");
		this.accessPolicy = Objects.requireNonNull(accessPolicy, "OrganizationAccessPolicy must not be null");
	}

	public EnquiryResponse createEnquiry(Long listingId, CreateEnquiryRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("CreateEnquiryRequest must not be null");
		}
		HostelOsPrincipal principal = accessPolicy.currentPrincipal();
		if (!principal.getRoles().contains("CUSTOMER")) {
			throw new OrganizationAccessDeniedException("A customer account is required to send an enquiry");
		}
		Listing listing = listingRepository.findByIdAndStatus(listingId, ListingStatus.PUBLISHED)
				.orElseThrow(() -> new ListingNotFoundException(listingId));
		if (listing.getOrganization().getStatus() != OrganizationStatus.ACTIVE) {
			throw new ListingNotFoundException(listingId);
		}
		if (listing.getOrganization().isOwnedBy(principal.getUserId())) {
			throw new OrganizationAccessDeniedException("Owners cannot enquire on their own listings");
		}
		String customerName = (request.customerName() == null || request.customerName().isBlank())
				? principal.getDisplayName()
				: request.customerName();
		String customerEmail = (request.customerEmail() == null || request.customerEmail().isBlank())
				? principal.getUsername()
				: request.customerEmail();
		Enquiry enquiry = new Enquiry(
				listing,
				principal.getUserId(),
				customerName,
				customerEmail,
				request.customerPhone(),
				request.message());
		return EnquiryResponse.from(enquiryRepository.saveAndFlush(enquiry));
	}

	@Transactional(readOnly = true)
	public List<EnquiryResponse> listEnquiries(Long organizationId, EnquiryStatus status) {
		Long userId = accessPolicy.currentUserId();
		accessPolicy.requireMemberAccess(organizationId, userId);
		List<Enquiry> enquiries = status == null
				? enquiryRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
				: enquiryRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, status);
		return enquiries.stream().map(EnquiryResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public EnquiryResponse getEnquiry(Long organizationId, Long enquiryId) {
		Long userId = accessPolicy.currentUserId();
		accessPolicy.requireMemberAccess(organizationId, userId);
		Enquiry enquiry = enquiryRepository.findByOrganizationIdAndId(organizationId, enquiryId)
				.orElseThrow(() -> new EnquiryNotFoundException(enquiryId));
		return EnquiryResponse.from(enquiry);
	}

	public EnquiryResponse updateStatus(Long organizationId, Long enquiryId, UpdateEnquiryStatusRequest request) {
		if (request == null || request.status() == null) {
			throw new IllegalArgumentException("Enquiry status must not be null");
		}
		Long userId = accessPolicy.currentUserId();
		accessPolicy.requireMemberAccess(organizationId, userId);
		Enquiry enquiry = enquiryRepository.findByIdAndOrganizationIdWithLock(enquiryId, organizationId)
				.orElseThrow(() -> new EnquiryNotFoundException(enquiryId));
		if (request.version() == null || enquiry.getVersion() != request.version()) {
			throw new EnquiryVersionConflictException(enquiryId);
		}
		enquiry.changeStatus(request.status());
		Enquiry saved = enquiryRepository.saveAndFlush(enquiry);
		return EnquiryResponse.from(saved);
	}
}
