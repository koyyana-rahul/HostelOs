package com.rahul.HostelOs.listing.dto;

import com.rahul.HostelOs.listing.AccommodationType;
import com.rahul.HostelOs.listing.Listing;
import com.rahul.HostelOs.listing.PricePeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Publicly discoverable listing details.")
public record PublicListingResponse(
		Long id,
		Long organizationId,
		String organizationName,
		String organizationSlug,
		String title,
		String description,
		AccommodationType accommodationType,
		String addressLine1,
		String city,
		String state,
		String postalCode,
		String country,
		BigDecimal latitude,
		BigDecimal longitude,
		BigDecimal basePrice,
		String currency,
		PricePeriod pricePeriod,
		String contactName,
		String contactEmail,
		String contactPhone,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static PublicListingResponse from(Listing listing) {
		return new PublicListingResponse(
				listing.getId(),
				listing.getOrganization().getId(),
				listing.getOrganization().getName(),
				listing.getOrganization().getSlug(),
				listing.getTitle(),
				listing.getDescription(),
				listing.getAccommodationType(),
				listing.getAddressLine1(),
				listing.getCity(),
				listing.getState(),
				listing.getPostalCode(),
				listing.getCountry(),
				listing.getLatitude(),
				listing.getLongitude(),
				listing.getBasePrice(),
				listing.getCurrency(),
				listing.getPricePeriod(),
				listing.getContactName(),
				listing.getContactEmail(),
				listing.getContactPhone(),
				listing.getCreatedAt(),
				listing.getUpdatedAt());
	}
}
