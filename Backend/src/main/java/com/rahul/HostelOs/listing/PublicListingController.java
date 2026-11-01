package com.rahul.HostelOs.listing;

import com.rahul.HostelOs.listing.dto.PublicListingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/listings")
@Tag(name = "Public Listings", description = "Public listing discovery")
public class PublicListingController {

	private final ListingService listingService;

	public PublicListingController(ListingService listingService) {
		this.listingService = listingService;
	}

	@GetMapping
	@Operation(summary = "Browse published listings")
	public Page<PublicListingResponse> search(
			@RequestParam(name = "q", required = false) String query,
			@RequestParam(name = "city", required = false) String city,
			@RequestParam(name = "type", required = false) AccommodationType type,
			@RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
			@RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "12") int size) {
		return listingService.searchPublished(query, city, type, minPrice, maxPrice, page, size);
	}

	@GetMapping("/{listingId}")
	@Operation(summary = "Get a published listing")
	public PublicListingResponse get(@PathVariable Long listingId) {
		return listingService.getPublishedListing(listingId);
	}
}
