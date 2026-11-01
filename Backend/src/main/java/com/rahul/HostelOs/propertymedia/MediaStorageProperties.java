package com.rahul.HostelOs.propertymedia;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "media")
public record MediaStorageProperties(
		String storageProvider,
		boolean enabled,
		long uploadSignatureTtlSeconds,
		long deliveryUrlTtlSeconds,
		int maxImagesPerProperty,
		long maxBytesPerImage,
		long maxPixelsPerImage,
		String allowedFormats) {

	public MediaStorageProperties {
		if (storageProvider == null || storageProvider.isBlank()) {
			throw new IllegalArgumentException("Media storage provider must not be blank");
		}
		if (uploadSignatureTtlSeconds <= 0 || deliveryUrlTtlSeconds <= 0
				|| maxImagesPerProperty <= 0 || maxBytesPerImage <= 0 || maxPixelsPerImage <= 0) {
			throw new IllegalArgumentException("Media storage limits and TTLs must be positive");
		}
	}

	public java.util.List<String> allowedFormatList() {
		if (allowedFormats == null || allowedFormats.isBlank()) {
			return java.util.List.of();
		}
		return java.util.Arrays.stream(allowedFormats.split(","))
				.map(String::trim)
				.filter(format -> !format.isBlank())
				.map(String::toLowerCase)
				.toList();
	}
}
