package com.rahul.HostelOs.listing.dto;

import com.rahul.HostelOs.listing.AccommodationType;
import com.rahul.HostelOs.listing.PricePeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Payload for updating a listing with optimistic concurrency control.")
public record UpdateListingRequest(
		@NotBlank(message = "Listing title must not be blank")
		@Size(min = 3, max = 160, message = "Listing title must be between 3 and 160 characters")
		String title,
		@Size(max = 4000, message = "Description must not exceed 4000 characters")
		String description,
		AccommodationType accommodationType,
		Long propertyId,
		@Size(max = 200, message = "Address line 1 must not exceed 200 characters")
		String addressLine1,
		@Size(max = 100, message = "City must not exceed 100 characters")
		String city,
		@Size(max = 100, message = "State must not exceed 100 characters")
		String state,
		@Size(max = 20, message = "Postal code must not exceed 20 characters")
		String postalCode,
		@Size(max = 100, message = "Country must not exceed 100 characters")
		String country,
		@DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
		@DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
		BigDecimal latitude,
		@DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
		@DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
		BigDecimal longitude,
		@DecimalMin(value = "0.0", message = "Base price must not be negative")
		BigDecimal basePrice,
		@Size(min = 3, max = 3, message = "Currency must be a three-letter code")
		String currency,
		PricePeriod pricePeriod,
		@Size(max = 120, message = "Contact name must not exceed 120 characters")
		String contactName,
		@Size(max = 254, message = "Contact email must not exceed 254 characters")
		String contactEmail,
		@Size(max = 40, message = "Contact phone must not exceed 40 characters")
		String contactPhone,
		@NotNull(message = "Version is required")
		Long version) {
}
