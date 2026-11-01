package com.rahul.HostelOs.propertymedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CloudinaryStorageProviderTests {

	private final CloudinaryProperties cloudinary = new CloudinaryProperties(
			"cloud",
			"key",
			"secret",
			"hostelos",
			30);
	private final MediaStorageProperties media = new MediaStorageProperties(
			"CLOUDINARY",
			true,
			600,
			900,
			20,
			10_485_760,
			25_000_000,
			"jpg,jpeg,png,webp");

	@Test
	void createsServerScopedStorageKeys() {
		CloudinaryStorageProvider provider = new CloudinaryStorageProvider(cloudinary, media, true);

		assertThat(provider.createStorageKey(new MediaStorageProvider.StorageKeyRequest(1, 2, 3)).value())
				.isEqualTo("hostelos/org-1/property-2/media-3");
	}

	@Test
	void signsOnlyParametersSentInTheUploadBody() {
		CloudinaryStorageProvider provider = new CloudinaryStorageProvider(cloudinary, media, true);

		MediaStorageProvider.UploadInstructions instructions = provider.createUploadInstructions(
				new MediaStorageProvider.UploadRequest(
						new MediaStorageProvider.StorageKey("hostelos/org-1/property-2/media-3"),
						"image/jpeg",
						100,
						null,
						java.time.Instant.now().plusSeconds(60)));

		assertThat(instructions.parameters())
				.containsKeys("public_id", "timestamp", "api_key", "signature")
				.doesNotContainKeys("resource_type", "type");
	}

	@Test
	void disabledProviderRejectsUploadInstructions() {
		CloudinaryStorageProvider provider = new CloudinaryStorageProvider(cloudinary, media, false);

		assertThatThrownBy(() -> provider.createUploadInstructions(new MediaStorageProvider.UploadRequest(
				new MediaStorageProvider.StorageKey("key"),
				"image/jpeg",
				100,
				null,
				java.time.Instant.now().plusSeconds(60))))
				.isInstanceOf(PropertyMediaStorageException.class)
				.hasMessageContaining("disabled");
	}

	@Test
	void missingCredentialsFailFast() {
		assertThatThrownBy(() -> new CloudinaryProperties("", "", "", "hostelos", 30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must be configured");
	}
}
