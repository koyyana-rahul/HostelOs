package com.rahul.HostelOs.propertymedia.dto;

import com.rahul.HostelOs.propertymedia.PropertyMedia;
import java.time.LocalDateTime;

public record PropertyMediaResponse(
		Long id,
		String providerType,
		String secureUrl,
		String mimeType,
		String format,
		long byteSize,
		Integer width,
		Integer height,
		int position,
		boolean primary,
		String status,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static PropertyMediaResponse from(PropertyMedia media) {
		return new PropertyMediaResponse(
				media.getId(),
				media.getProviderType().name(),
				media.getSecureUrl(),
				media.getMimeType(),
				media.getFormat(),
				media.getByteSize(),
				media.getWidth(),
				media.getHeight(),
				media.getPosition(),
				media.isPrimaryImage(),
				media.getStatus().name(),
				media.getCreatedAt(),
				media.getUpdatedAt());
	}
}
