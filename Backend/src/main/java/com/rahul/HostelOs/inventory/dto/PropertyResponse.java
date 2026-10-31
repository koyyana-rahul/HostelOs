package com.rahul.HostelOs.inventory.dto;

import com.rahul.HostelOs.inventory.Property;
import com.rahul.HostelOs.inventory.PropertyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Property details response.")
public record PropertyResponse(
		@Schema(description = "Unique property identifier.", example = "1")
		Long id,

		@Schema(description = "Parent organization identifier.", example = "10")
		Long organizationId,

		@Schema(description = "Human-readable property name.", example = "Green Valley Hostel")
		String name,

		@Schema(description = "Organization-scoped unique property code.", example = "GVH-01")
		String code,

		@Schema(description = "Property operational status.", example = "ACTIVE")
		PropertyStatus status,

		@Schema(description = "Primary address line.", example = "Plot 42, North Campus Road")
		String addressLine1,

		@Schema(description = "Secondary address line or landmark.", example = "Near University Gate")
		String addressLine2,

		@Schema(description = "City.", example = "Bengaluru")
		String city,

		@Schema(description = "State or Province.", example = "Karnataka")
		String state,

		@Schema(description = "Postal or ZIP code.", example = "560001")
		String postalCode,

		@Schema(description = "Country ISO code or name.", example = "IN")
		String country,

		@Schema(description = "IANA timezone ID.", example = "Asia/Kolkata")
		String timezone,

		@Schema(description = "Entity version for optimistic locking.")
		long version,

		@Schema(description = "Timestamp when the property was created.")
		LocalDateTime createdAt,

		@Schema(description = "Timestamp when the property was last updated.")
		LocalDateTime updatedAt) {

	public static PropertyResponse from(Property property) {
		return new PropertyResponse(
				property.getId(),
				property.getOrganization().getId(),
				property.getName(),
				property.getCode(),
				property.getStatus(),
				property.getAddressLine1(),
				property.getAddressLine2(),
				property.getCity(),
				property.getState(),
				property.getPostalCode(),
				property.getCountry(),
				property.getTimezone(),
				property.getVersion(),
				property.getCreatedAt(),
				property.getUpdatedAt());
	}
}
