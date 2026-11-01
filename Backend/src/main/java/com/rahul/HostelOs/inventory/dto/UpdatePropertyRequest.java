package com.rahul.HostelOs.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Payload for updating mutable property configuration.")
public record UpdatePropertyRequest(
		@NotBlank(message = "Property name must not be blank")
		@Size(max = 120, message = "Property name must not exceed 120 characters")
		@Schema(description = "Updated human-readable property name.", example = "Green Valley Hostel Main")
		String name,

		@Size(max = 200, message = "Address line 1 must not exceed 200 characters")
		@Schema(description = "Updated primary address line.", example = "Plot 42, North Campus Road")
		String addressLine1,

		@Size(max = 200, message = "Address line 2 must not exceed 200 characters")
		@Schema(description = "Updated secondary address line.", example = "Near University Gate")
		String addressLine2,

		@NotBlank(message = "City must not be blank")
		@Size(max = 100, message = "City must not exceed 100 characters")
		@Schema(description = "Updated city.", example = "Bengaluru")
		String city,

		@Size(max = 100, message = "State must not exceed 100 characters")
		@Schema(description = "Updated state or province.", example = "Karnataka")
		String state,

		@Size(max = 20, message = "Postal code must not exceed 20 characters")
		@Schema(description = "Updated postal code.", example = "560001")
		String postalCode,

		@Size(max = 100, message = "Country must not exceed 100 characters")
		@Schema(description = "Updated country.", example = "IN")
		String country,

		@Size(max = 50, message = "Timezone must not exceed 50 characters")
		@Schema(description = "Updated IANA timezone ID.", example = "Asia/Kolkata")
		String timezone,

		@DecimalMax(value = "90", message = "Latitude must not exceed 90 degrees")
		@DecimalMin(value = "-90", message = "Latitude must not be less than -90 degrees")
		@Schema(description = "Updated property latitude in decimal degrees.", example = "12.9719")
		BigDecimal latitude,

		@DecimalMax(value = "180", message = "Longitude must not exceed 180 degrees")
		@DecimalMin(value = "-180", message = "Longitude must not be less than -180 degrees")
		@Schema(description = "Updated property longitude in decimal degrees.", example = "77.5937")
		BigDecimal longitude,

		@Size(max = 64, message = "Place ID must not exceed 64 characters")
		@Schema(description = "Place ID from geocoding provider.", example = "ChIJd8BlQ2BZwokRAFUEcm_qryI")
		String placeId,

		@Size(max = 50, message = "Location provider must not exceed 50 characters")
		@Schema(description = "Geocoding provider used.", example = "openstreetmap")
		String locationProvider) {
}
