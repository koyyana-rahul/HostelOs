package com.rahul.HostelOs.enquiry;

import com.rahul.HostelOs.enquiry.dto.CreateEnquiryRequest;
import com.rahul.HostelOs.enquiry.dto.CustomerEnquiryResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

	@Transactional(readOnly = true)
	public Page<CustomerEnquiryResponse> listCustomerEnquiries(
			EnquiryStatus status,
			Pageable pageable) {
		Long customerUserId = accessPolicy.currentPrincipal().getUserId();
		Pageable safePageable = safePageable(pageable);
		Page<Enquiry> enquiries = status == null
				? enquiryRepository.findByCustomerUserIdOrderByCreatedAtDesc(customerUserId, safePageable)
				: enquiryRepository.findByCustomerUserIdAndStatusOrderByCreatedAtDesc(
						customerUserId,
						status,
						safePageable);
		return enquiries.map(CustomerEnquiryResponse::from);
	}

	@Transactional(readOnly = true)
	public CustomerEnquiryResponse getCustomerEnquiry(Long enquiryId) {
		Long customerUserId = accessPolicy.currentPrincipal().getUserId();
		Enquiry enquiry = enquiryRepository.findByCustomerUserIdAndId(customerUserId, enquiryId)
				.orElseThrow(() -> new EnquiryNotFoundException(enquiryId));
		return CustomerEnquiryResponse.from(enquiry);
	}

	private Pageable safePageable(Pageable pageable) {
		int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
		int size = pageable == null ? 20 : Math.min(Math.max(pageable.getPageSize(), 1), 50);
		Sort sort = pageable != null && pageable.getSort().isSorted()
				? pageable.getSort()
				: Sort.by(Sort.Direction.DESC, "createdAt");
		return PageRequest.of(page, size, sort);
	}
}
