package com.rahul.HostelOs.enquiry;

import com.rahul.HostelOs.enquiry.dto.CreateEnquiryRequest;
import com.rahul.HostelOs.enquiry.dto.CustomerEnquiryResponse;
import com.rahul.HostelOs.enquiry.dto.EnquiryResponse;
import com.rahul.HostelOs.enquiry.dto.UpdateEnquiryStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Enquiries", description = "Customer enquiries and owner inbox")
public class EnquiryController {

	private final EnquiryService enquiryService;

	public EnquiryController(EnquiryService enquiryService) {
		this.enquiryService = enquiryService;
	}

	@PostMapping("/api/v1/listings/{listingId}/enquiries")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Send an enquiry for a published listing")
	public EnquiryResponse createEnquiry(
			@PathVariable Long listingId,
			@Valid @RequestBody CreateEnquiryRequest request) {
		return enquiryService.createEnquiry(listingId, request);
	}

	@GetMapping("/api/v1/customers/me/enquiries")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "List the authenticated customer's enquiries")
	public Page<CustomerEnquiryResponse> listCustomerEnquiries(
			@RequestParam(name = "status", required = false) EnquiryStatus status,
			@PageableDefault(size = 20) Pageable pageable) {
		return enquiryService.listCustomerEnquiries(status, pageable);
	}

	@GetMapping("/api/v1/customers/me/enquiries/{enquiryId}")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Get an enquiry owned by the authenticated customer")
	public CustomerEnquiryResponse getCustomerEnquiry(@PathVariable Long enquiryId) {
		return enquiryService.getCustomerEnquiry(enquiryId);
	}

	@GetMapping("/api/v1/organizations/{organizationId}/enquiries")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
	@Operation(summary = "List organization enquiries")
	public List<EnquiryResponse> listEnquiries(
			@PathVariable Long organizationId,
			@RequestParam(name = "status", required = false) EnquiryStatus status) {
		return enquiryService.listEnquiries(organizationId, status);
	}

	@GetMapping("/api/v1/organizations/{organizationId}/enquiries/{enquiryId}")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
	@Operation(summary = "Get an enquiry")
	public EnquiryResponse getEnquiry(
			@PathVariable Long organizationId,
			@PathVariable Long enquiryId) {
		return enquiryService.getEnquiry(organizationId, enquiryId);
	}

	@PatchMapping("/api/v1/organizations/{organizationId}/enquiries/{enquiryId}/status")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
	@Operation(summary = "Update enquiry workflow status")
	public EnquiryResponse updateStatus(
			@PathVariable Long organizationId,
			@PathVariable Long enquiryId,
			@Valid @RequestBody UpdateEnquiryStatusRequest request) {
		return enquiryService.updateStatus(organizationId, enquiryId, request);
	}
}
