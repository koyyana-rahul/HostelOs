package com.rahul.HostelOs.propertymedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rahul.HostelOs.inventory.Property;
import com.rahul.HostelOs.organization.Organization;
import org.junit.jupiter.api.Test;
import java.time.Instant;

class PropertyMediaTests {

	@Test
	void pendingMediaTransitionsToReadyWithVerifiedMetadata() {
		Organization organization = mock(Organization.class);
		Property property = mock(Property.class);
		when(organization.getId()).thenReturn(10L);
		when(property.getOrganization()).thenReturn(organization);
		PropertyMedia media = new PropertyMedia(
				organization,
				property,
				MediaProviderType.CLOUDINARY,
				"org-10/property-20/media-30",
				"image/jpeg",
				"jpg",
				1200L,
				0);

		media.markReady("asset-1", "https://cdn.example/image.jpg", "checksum", 800, 600);

		assertThat(media.getStatus()).isEqualTo(PropertyMediaStatus.READY);
		assertThat(media.getProviderAssetId()).isEqualTo("asset-1");
		assertThat(media.getWidth()).isEqualTo(800);
		assertThat(media.getHeight()).isEqualTo(600);
	}

	@Test
	void mediaRejectsAPropertyFromAnotherOrganization() {
		Organization organization = mock(Organization.class);
		Organization otherOrganization = mock(Organization.class);
		Property property = mock(Property.class);
		when(organization.getId()).thenReturn(10L);
		when(otherOrganization.getId()).thenReturn(11L);
		when(property.getOrganization()).thenReturn(otherOrganization);

		assertThatThrownBy(() -> new PropertyMedia(
				organization,
				property,
				MediaProviderType.CLOUDINARY,
				"key",
				"image/png",
				"png",
				1200L,
				0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("organization");
	}

	@Test
	void expiredUploadIntentIsDetected() {
		Organization organization = mock(Organization.class);
		Property property = mock(Property.class);
		when(organization.getId()).thenReturn(10L);
		when(property.getOrganization()).thenReturn(organization);
		PropertyMedia media = new PropertyMedia(
				organization,
				property,
				MediaProviderType.CLOUDINARY,
				"key",
				"image/jpeg",
				"jpg",
				1200L,
				0,
				Instant.now().minusSeconds(1));

		assertThat(media.isUploadExpired(Instant.now())).isTrue();
	}

	@Test
	void uploadChecksumIsBoundToTheIntent() {
		Organization organization = mock(Organization.class);
		Property property = mock(Property.class);
		when(organization.getId()).thenReturn(10L);
		when(property.getOrganization()).thenReturn(organization);

		PropertyMedia media = new PropertyMedia(
				organization,
				property,
				MediaProviderType.CLOUDINARY,
				"key",
				"image/jpeg",
				"jpg",
				1200L,
				0,
				Instant.now().plusSeconds(60),
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

		assertThat(media.getUploadChecksumSha256())
				.isEqualTo("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
	}
}
