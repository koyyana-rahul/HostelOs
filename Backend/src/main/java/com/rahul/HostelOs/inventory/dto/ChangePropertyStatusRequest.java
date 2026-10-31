package com.rahul.HostelOs.inventory.dto;

import com.rahul.HostelOs.inventory.PropertyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for transitioning a property status.")
public record ChangePropertyStatusRequest(
		@NotNull(message = "Property status must not be null")
		@Schema(description = "Target operational status for the property.", example = "ACTIVE")
		PropertyStatus status) {
}
