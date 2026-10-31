package com.rahul.HostelOs.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for creating a new property.")
public record CreatePropertyRequest(
		@NotBlank(message = "Property name must not be blank")
		@Size(max = 120, message = "Property name must not exceed 120 characters")
		@Schema(description = "Human-readable property name.", example = "Green Valley Hostel")
		String name,

		@NotBlank(message = "Property code must not be blank")
		@Size(min = 2, max = 30, message = "Property code must be between 2 and 30 characters")
		@Pattern(regexp = "^[A-Za-z0-9_-]{2,30}$", message = "Property code must consist of alphanumeric characters, underscores, or hyphens")
		@Schema(description = "Organization-scoped unique property code.", example = "GVH-01")
		String code,

		@Size(max = 200, message = "Address line 1 must not exceed 200 characters")
		@Schema(description = "Primary address line.", example = "Plot 42, North Campus Road")
		String addressLine1,

		@Size(max = 200, message = "Address line 2 must not exceed 200 characters")
		@Schema(description = "Secondary address line or landmark.", example = "Near University Gate")
		String addressLine2,

		@NotBlank(message = "City must not be blank")
		@Size(max = 100, message = "City must not exceed 100 characters")
		@Schema(description = "City where the property is located.", example = "Bengaluru")
		String city,

		@Size(max = 100, message = "State must not exceed 100 characters")
		@Schema(description = "State or Province.", example = "Karnataka")
		String state,

		@Size(max = 20, message = "Postal code must not exceed 20 characters")
		@Schema(description = "Postal or ZIP code.", example = "560001")
		String postalCode,

		@Size(max = 100, message = "Country must not exceed 100 characters")
		@Schema(description = "Country.", example = "IN")
		String country,

		@Size(max = 50, message = "Timezone must not exceed 50 characters")
		@Schema(description = "IANA timezone ID.", example = "Asia/Kolkata")
		String timezone) {
}
