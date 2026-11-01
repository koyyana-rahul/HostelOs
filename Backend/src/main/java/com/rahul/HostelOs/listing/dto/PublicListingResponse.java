package com.rahul.HostelOs.listing.dto;

import com.rahul.HostelOs.listing.AccommodationType;
import com.rahul.HostelOs.listing.Listing;
import com.rahul.HostelOs.listing.PricePeriod;
import com.rahul.HostelOs.propertymedia.PropertyMedia;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
		LocalDateTime updatedAt,
		PublicListingImage primaryImage,
		List<PublicListingImage> images,
		int imageCount) {

	public static PublicListingResponse from(Listing listing) {
		return from(listing, List.of());
	}

	public static PublicListingResponse from(Listing listing, List<PropertyMedia> media) {
		List<PublicListingImage> images = media.stream()
				.filter(item -> item.getStatus() == com.rahul.HostelOs.propertymedia.PropertyMediaStatus.READY)
				.filter(item -> item.getProperty() != null
						&& listing.getProperty() != null
						&& item.getProperty().getId().equals(listing.getProperty().getId())
						&& item.getOrganization().getId().equals(listing.getOrganization().getId()))
				.map(item -> new PublicListingImage(
						item.getSecureUrl(),
						listing.getTitle(),
						item.getPosition()))
				.toList();
		PublicListingImage primaryImage = media.stream()
				.filter(item -> item.getStatus() == com.rahul.HostelOs.propertymedia.PropertyMediaStatus.READY)
				.filter(PropertyMedia::isPrimaryImage)
				.filter(item -> item.getProperty() != null
						&& listing.getProperty() != null
						&& item.getProperty().getId().equals(listing.getProperty().getId())
						&& item.getOrganization().getId().equals(listing.getOrganization().getId()))
				.map(item -> new PublicListingImage(item.getSecureUrl(), listing.getTitle(), item.getPosition()))
				.findFirst()
				.orElse(images.isEmpty() ? null : images.get(0));
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
				listing.getUpdatedAt(),
				primaryImage,
				images,
				images.size());
	}

	public record PublicListingImage(String url, String altText, int displayOrder) {
	}
}
