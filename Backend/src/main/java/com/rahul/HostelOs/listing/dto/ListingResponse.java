package com.rahul.HostelOs.listing.dto;

import com.rahul.HostelOs.listing.AccommodationType;
import com.rahul.HostelOs.listing.Listing;
import com.rahul.HostelOs.listing.ListingStatus;
import com.rahul.HostelOs.listing.PricePeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Organization-scoped listing details.")
public record ListingResponse(
		Long id,
		Long organizationId,
		Long propertyId,
		String propertyName,
		String title,
		String description,
		AccommodationType accommodationType,
		ListingStatus status,
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
		long version,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		LocalDateTime archivedAt) {

	public static ListingResponse from(Listing listing) {
		return new ListingResponse(
				listing.getId(),
				listing.getOrganization().getId(),
				listing.getProperty() == null ? null : listing.getProperty().getId(),
				listing.getProperty() == null ? null : listing.getProperty().getName(),
				listing.getTitle(),
				listing.getDescription(),
				listing.getAccommodationType(),
				listing.getStatus(),
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
				listing.getVersion(),
				listing.getCreatedAt(),
				listing.getUpdatedAt(),
				listing.getArchivedAt());
	}
}
