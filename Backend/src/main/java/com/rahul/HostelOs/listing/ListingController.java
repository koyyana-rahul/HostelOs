package com.rahul.HostelOs.listing;

import com.rahul.HostelOs.listing.dto.ChangeListingStatusRequest;
import com.rahul.HostelOs.listing.dto.CreateListingRequest;
import com.rahul.HostelOs.listing.dto.ListingResponse;
import com.rahul.HostelOs.listing.dto.UpdateListingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/listings")
@Tag(name = "Listings", description = "Owner listing management")
public class ListingController {

	private final ListingService listingService;

	public ListingController(ListingService listingService) {
		this.listingService = listingService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Create a listing draft")
	public ListingResponse createListing(
			@PathVariable Long organizationId,
			@Valid @RequestBody CreateListingRequest request) {
		return listingService.createListing(organizationId, request);
	}

	@GetMapping
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
	@Operation(summary = "List organization listings")
	public List<ListingResponse> listListings(
			@PathVariable Long organizationId,
			@RequestParam(name = "status", required = false) ListingStatus status) {
		return listingService.listListings(organizationId, status);
	}

	@GetMapping("/{listingId}")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
	@Operation(summary = "Get a listing")
	public ListingResponse getListing(
			@PathVariable Long organizationId,
			@PathVariable Long listingId) {
		return listingService.getListing(organizationId, listingId);
	}

	@PutMapping("/{listingId}")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Update a listing")
	public ListingResponse updateListing(
			@PathVariable Long organizationId,
			@PathVariable Long listingId,
			@Valid @RequestBody UpdateListingRequest request) {
		return listingService.updateListing(organizationId, listingId, request);
	}

	@PatchMapping("/{listingId}/status")
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Publish, unpublish, or archive a listing")
	public ListingResponse changeStatus(
			@PathVariable Long organizationId,
			@PathVariable Long listingId,
			@Valid @RequestBody ChangeListingStatusRequest request) {
		return listingService.changeStatus(organizationId, listingId, request);
	}

	@DeleteMapping("/{listingId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
	@Operation(summary = "Archive a listing")
	public ResponseEntity<Void> archiveListing(
			@PathVariable Long organizationId,
			@PathVariable Long listingId,
			@RequestParam("version") Long version) {
		listingService.archiveListing(organizationId, listingId, version);
		return ResponseEntity.noContent().build();
	}
}
