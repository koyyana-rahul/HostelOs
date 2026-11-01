package com.rahul.HostelOs.propertymedia;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
		String cloudName,
		String apiKey,
		String apiSecret,
		String folderPrefix,
		int timeoutSeconds) {

	public CloudinaryProperties {
		if (cloudName == null || cloudName.isBlank()
				|| apiKey == null || apiKey.isBlank()
				|| apiSecret == null || apiSecret.isBlank()) {
			throw new IllegalArgumentException(
					"Cloudinary cloud name, API key, and API secret must be configured");
		}
		if (folderPrefix == null || folderPrefix.isBlank()) {
			throw new IllegalArgumentException("Cloudinary folder prefix must not be blank");
		}
		if (timeoutSeconds <= 0) {
			throw new IllegalArgumentException("Cloudinary timeout must be positive");
		}
	}
}
