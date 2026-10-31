package com.rahul.HostelOs.listing.dto;

import com.rahul.HostelOs.listing.ListingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for changing listing lifecycle state.")
public record ChangeListingStatusRequest(
		@NotNull(message = "Listing status is required")
		ListingStatus status,
		@NotNull(message = "Version is required")
		Long version) {
}
