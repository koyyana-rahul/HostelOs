package com.rahul.HostelOs.propertymedia;

import java.util.Locale;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({MediaStorageProperties.class, CloudinaryProperties.class})
public class PropertyMediaConfiguration {

	@Bean
	MediaStorageProvider mediaStorageProvider(
			MediaStorageProperties mediaStorageProperties,
			CloudinaryProperties cloudinaryProperties) {
		if (!"CLOUDINARY".equals(mediaStorageProperties.storageProvider().trim().toUpperCase(Locale.ROOT))) {
			throw new IllegalStateException(
					"Unsupported media storage provider: " + mediaStorageProperties.storageProvider());
		}
		return new CloudinaryStorageProvider(
				cloudinaryProperties,
				mediaStorageProperties,
				mediaStorageProperties.enabled());
	}
}
