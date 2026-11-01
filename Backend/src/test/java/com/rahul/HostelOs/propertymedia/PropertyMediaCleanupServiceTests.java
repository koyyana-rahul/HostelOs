package com.rahul.HostelOs.propertymedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PropertyMediaCleanupServiceTests {

	@Test
	void cleanupClaimsAndDeletesOnlyOnce() {
		PropertyMediaRepository repository = mock(PropertyMediaRepository.class);
		MediaStorageProvider provider = mock(MediaStorageProvider.class);
		PropertyMedia media = expiredMedia();
		when(repository.findExpiredByStatus(any(), any(), any())).thenReturn(List.of(media), List.of());
		when(repository.claimExpiredForCleanup(any(), any(), any(), any())).thenReturn(1);
		when(repository.findByIdWithLock(1L)).thenReturn(Optional.of(media));

		new PropertyMediaCleanupService(repository, provider).cleanExpiredUploads();

		verify(provider).delete(new MediaStorageProvider.DeleteRequest(
				null, new MediaStorageProvider.StorageKey("server-owned-key")));
		verify(media).markDeleted();
	}

	@Test
	void concurrentWorkerThatLosesClaimDoesNotDelete() {
		PropertyMediaRepository repository = mock(PropertyMediaRepository.class);
		MediaStorageProvider provider = mock(MediaStorageProvider.class);
		PropertyMedia media = expiredMedia();
		when(repository.findExpiredByStatus(any(), any(), any())).thenReturn(List.of(media), List.of());
		when(repository.claimExpiredForCleanup(any(), any(), any(), any())).thenReturn(0);

		new PropertyMediaCleanupService(repository, provider).cleanExpiredUploads();

		org.mockito.Mockito.verifyNoInteractions(provider);
	}

	@Test
	void failedProviderDeletionReturnsClaimToRetryableState() {
		PropertyMediaRepository repository = mock(PropertyMediaRepository.class);
		MediaStorageProvider provider = mock(MediaStorageProvider.class);
		PropertyMedia media = expiredMedia();
		when(repository.findExpiredByStatus(any(), any(), any())).thenReturn(List.of(media), List.of());
		when(repository.claimExpiredForCleanup(any(), any(), any(), any())).thenReturn(1);
		when(repository.findByIdWithLock(1L)).thenReturn(Optional.of(media));
		doThrow(new PropertyMediaStorageException("provider unavailable")).when(provider).delete(any());

		new PropertyMediaCleanupService(repository, provider).cleanExpiredUploads();

		verify(media).markFailed();
		verify(provider).delete(any());
	}

	private static PropertyMedia expiredMedia() {
		PropertyMedia media = mock(PropertyMedia.class);
		when(media.getId()).thenReturn(1L);
		when(media.getStatus()).thenReturn(PropertyMediaStatus.DELETING);
		when(media.getProviderAssetId()).thenReturn(null);
		when(media.getProviderStorageKey()).thenReturn("server-owned-key");
		return media;
	}
}
