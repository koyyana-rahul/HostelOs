package com.rahul.HostelOs.propertymedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rahul.HostelOs.inventory.Property;
import com.rahul.HostelOs.inventory.PropertyRepository;
import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.organization.OrganizationAccessPolicy;
import com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropertyMediaServiceTests {

	private PropertyMediaRepository mediaRepository;
	private PropertyRepository propertyRepository;
	private OrganizationAccessPolicy accessPolicy;
	private MediaStorageProvider storageProvider;
	private PropertyMediaService service;
	private Organization organization;
	private Property property;

	@BeforeEach
	void setUp() {
		mediaRepository = mock(PropertyMediaRepository.class);
		propertyRepository = mock(PropertyRepository.class);
		accessPolicy = mock(OrganizationAccessPolicy.class);
		storageProvider = mock(MediaStorageProvider.class);
		organization = mock(Organization.class);
		property = mock(Property.class);
		when(organization.getId()).thenReturn(10L);
		when(property.getOrganization()).thenReturn(organization);
		when(accessPolicy.currentUserId()).thenReturn(7L);
		when(accessPolicy.requireOwnerOrAdmin(10L, 7L)).thenReturn(organization);
		when(propertyRepository.findByOrganizationIdAndId(10L, 20L)).thenReturn(Optional.of(property));
		when(storageProvider.providerType()).thenReturn("CLOUDINARY");
		service = new PropertyMediaService(
				mediaRepository,
				propertyRepository,
				accessPolicy,
				storageProvider,
				new MediaStorageProperties("CLOUDINARY", true, 600, 900, 20, 10_485_760, 25_000_000,
						"jpg,jpeg,png,webp"));
	}

	@Test
	void rejectsUnauthorizedOrganizationBeforePropertyLookup() {
		doThrow(new OrganizationAccessDeniedException("denied"))
				.when(accessPolicy).requireOwnerOrAdmin(10L, 7L);

		assertThatThrownBy(() -> service.listReadyMedia(10L, 20L))
				.isInstanceOf(OrganizationAccessDeniedException.class);

		verifyNoInteractions(propertyRepository, mediaRepository);
	}

	@Test
	void rejectsPropertyBelongingToAnotherOrganization() {
		Organization otherOrganization = mock(Organization.class);
		when(otherOrganization.getId()).thenReturn(11L);
		when(property.getOrganization()).thenReturn(otherOrganization);

		assertThatThrownBy(() -> service.createUploadIntent(
				10L, 20L, new PropertyMediaService.CreateUploadIntentRequest("image/jpeg", 100, null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("organization");

	}

	@Test
	void rejectsExpiredCompletionAndAttemptsServerOwnedCleanup() {
		PropertyMedia media = mockMedia(PropertyMediaStatus.PENDING, true);
		when(mediaRepository.findByOrganizationIdAndPropertyIdAndIdWithLock(10L, 20L, 30L))
				.thenReturn(Optional.of(media));

		assertThatThrownBy(() -> service.completeUpload(
				10L, 20L, 30L, new PropertyMediaService.CompleteUploadRequest("client-id", "client-url", null)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("expired");

		verify(media).markFailed();
		verify(storageProvider).delete(any());
		verify(storageProvider, never()).verifyUploadedAsset(any());
	}

	@Test
	void duplicateCompletionReturnsReadyMediaWithoutProviderCall() {
		PropertyMedia media = mockMedia(PropertyMediaStatus.READY, false);
		when(mediaRepository.findByOrganizationIdAndPropertyIdAndIdWithLock(10L, 20L, 30L))
				.thenReturn(Optional.of(media));

		assertThat(service.completeUpload(
				10L, 20L, 30L, new PropertyMediaService.CompleteUploadRequest("client-id", "client-url", null)))
				.isSameAs(media);

		verifyNoInteractions(storageProvider);
	}

	@Test
	void providerVerificationFailureMarksMediaFailedAndCleansUp() {
		PropertyMedia media = mockMedia(PropertyMediaStatus.PENDING, false);
		when(mediaRepository.findByOrganizationIdAndPropertyIdAndIdWithLock(10L, 20L, 30L))
				.thenReturn(Optional.of(media));
		doThrow(new PropertyMediaStorageException("verification failed"))
				.when(storageProvider).verifyUploadedAsset(any());

		assertThatThrownBy(() -> service.completeUpload(
				10L, 20L, 30L, new PropertyMediaService.CompleteUploadRequest("client-id", "client-url", null)))
				.isInstanceOf(PropertyMediaStorageException.class);

		verify(media).markFailed();
		verify(storageProvider).delete(any());
	}

	private PropertyMedia mockMedia(PropertyMediaStatus status, boolean expired) {
		PropertyMedia media = mock(PropertyMedia.class);
		when(media.getStatus()).thenReturn(status);
		when(media.isUploadExpired(any(Instant.class))).thenReturn(expired);
		when(media.getProviderStorageKey()).thenReturn("server-owned-key");
		when(media.getMimeType()).thenReturn("image/jpeg");
		when(media.getByteSize()).thenReturn(100L);
		return media;
	}
}
