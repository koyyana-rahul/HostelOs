package com.rahul.HostelOs.propertymedia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Provider response submitted to complete and verify an image upload.")
public record CompleteUploadRequest(
		@NotBlank
		@Size(max = 255)
		String providerAssetId,
		@NotBlank
		@Size(max = 1000)
		String providerAssetUrl,
		@Size(max = 64)
		String checksumSha256) {
}
