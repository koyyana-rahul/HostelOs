package com.rahul.HostelOs.propertymedia.dto;

import com.rahul.HostelOs.propertymedia.MediaStorageProvider;
import java.time.Instant;
import java.util.Map;

public record UploadIntentResponse(
		Long mediaId,
		String providerType,
		String method,
		String uploadUrl,
		Map<String, String> parameters,
		Instant expiresAt,
		long maxBytes) {

	public static UploadIntentResponse from(
			Long mediaId,
			String providerType,
			MediaStorageProvider.UploadInstructions instructions) {
		return new UploadIntentResponse(
				mediaId,
				providerType,
				instructions.method(),
				instructions.uploadUrl(),
				instructions.parameters(),
				instructions.expiresAt(),
				instructions.maxBytes());
	}
}
