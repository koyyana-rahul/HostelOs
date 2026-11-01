package com.rahul.HostelOs.propertymedia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Metadata used to create a signed property image upload.")
public record CreateUploadIntentRequest(
		@NotBlank
		@Size(max = 100)
		String mimeType,
		@Positive
		long byteSize,
		@Size(max = 64)
		String checksumSha256) {
}
