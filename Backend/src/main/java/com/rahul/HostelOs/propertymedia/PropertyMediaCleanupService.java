package com.rahul.HostelOs.propertymedia;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PropertyMediaCleanupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropertyMediaCleanupService.class);
	private static final int CLEANUP_BATCH_SIZE = 100;
	private final PropertyMediaRepository mediaRepository;
	private final MediaStorageProvider storageProvider;

	public PropertyMediaCleanupService(
			PropertyMediaRepository mediaRepository,
			MediaStorageProvider storageProvider) {
		this.mediaRepository = mediaRepository;
		this.storageProvider = storageProvider;
	}

	@Scheduled(fixedDelayString = "${media.cleanup-interval-ms:3600000}")
	public void cleanExpiredUploads() {
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		cleanup(PropertyMediaStatus.PENDING, now);
		cleanup(PropertyMediaStatus.FAILED, now);
	}

	private void cleanup(PropertyMediaStatus expectedStatus, LocalDateTime cutoff) {
		List<PropertyMedia> mediaItems = mediaRepository.findExpiredByStatus(
				expectedStatus, cutoff, PageRequest.of(0, CLEANUP_BATCH_SIZE));
		for (PropertyMedia media : mediaItems) {
			// DELETING rows are intentionally not reclaimed automatically; a future lease column
			// can support crash recovery without risking concurrent provider deletion.
			if (media.getId() == null || mediaRepository.claimExpiredForCleanup(
					media.getId(), expectedStatus, PropertyMediaStatus.DELETING, cutoff) != 1) {
				continue;
			}
			Long mediaId = media.getId();
			try {
				media = mediaRepository.findByIdWithLock(mediaId).orElseThrow();
				storageProvider.delete(new MediaStorageProvider.DeleteRequest(
						media.getProviderAssetId(),
						new MediaStorageProvider.StorageKey(media.getProviderStorageKey())));
				media.markDeleted();
				mediaRepository.saveAndFlush(media);
			} catch (RuntimeException exception) {
				mediaRepository.findByIdWithLock(mediaId).ifPresent(claimed -> {
					if (claimed.getStatus() == PropertyMediaStatus.DELETING) {
						claimed.markFailed();
						mediaRepository.saveAndFlush(claimed);
					}
				});
				LOGGER.warn("Unable to clean up expired property media {}", mediaId, exception);
			}
		}
	}
}
