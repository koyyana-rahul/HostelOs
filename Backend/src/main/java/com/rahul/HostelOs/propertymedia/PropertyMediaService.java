package com.rahul.HostelOs.propertymedia;

import com.rahul.HostelOs.inventory.Property;
import com.rahul.HostelOs.inventory.PropertyRepository;
import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.organization.OrganizationAccessPolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class PropertyMediaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropertyMediaService.class);
	private static final Collection<PropertyMediaStatus> ACTIVE_STATUSES = List.of(
			PropertyMediaStatus.PENDING,
			PropertyMediaStatus.READY,
			PropertyMediaStatus.DELETING);

	private final PropertyMediaRepository mediaRepository;
	private final PropertyRepository propertyRepository;
	private final OrganizationAccessPolicy accessPolicy;
	private final MediaStorageProvider storageProvider;
	private final MediaStorageProperties storageProperties;

	public PropertyMediaService(
			PropertyMediaRepository mediaRepository,
			PropertyRepository propertyRepository,
			OrganizationAccessPolicy accessPolicy,
			MediaStorageProvider storageProvider,
			MediaStorageProperties storageProperties) {
		this.mediaRepository = Objects.requireNonNull(mediaRepository, "PropertyMediaRepository must not be null");
		this.propertyRepository = Objects.requireNonNull(propertyRepository, "PropertyRepository must not be null");
		this.accessPolicy = Objects.requireNonNull(accessPolicy, "OrganizationAccessPolicy must not be null");
		this.storageProvider = Objects.requireNonNull(storageProvider, "MediaStorageProvider must not be null");
		this.storageProperties = Objects.requireNonNull(storageProperties, "MediaStorageProperties must not be null");
	}

	public UploadIntent createUploadIntent(
			Long organizationId,
			Long propertyId,
			CreateUploadIntentRequest request) {
		Organization organization = authorizeOwner(organizationId);
		Property property = requireProperty(organizationId, propertyId);
		validateUploadRequest(request);
		long activeCount = mediaRepository.countByOrganizationIdAndPropertyIdAndStatusIn(
				organizationId, propertyId, ACTIVE_STATUSES);
		if (activeCount >= storageProperties.maxImagesPerProperty()) {
			throw new IllegalArgumentException("Property image limit has been reached");
		}

		PropertyMedia media = new PropertyMedia(
				organization,
				property,
				MediaProviderType.valueOf(storageProvider.providerType()),
				"pending/" + UUID.randomUUID(),
				request.mimeType(),
				formatFromMimeType(request.mimeType()),
				request.byteSize(),
				(int) activeCount,
				Instant.now().plusSeconds(storageProperties.uploadSignatureTtlSeconds()),
				request.checksumSha256());
		PropertyMedia saved = mediaRepository.saveAndFlush(media);
		MediaStorageProvider.StorageKey storageKey = storageProvider.createStorageKey(
				new MediaStorageProvider.StorageKeyRequest(organizationId, propertyId, saved.getId()));
		saved.replaceProviderStorageKey(storageKey.value());
		mediaRepository.saveAndFlush(saved);

		MediaStorageProvider.UploadInstructions instructions = storageProvider.createUploadInstructions(
				new MediaStorageProvider.UploadRequest(
						storageKey,
						request.mimeType(),
						request.byteSize(),
						request.checksumSha256(),
						Instant.now().plusSeconds(storageProperties.uploadSignatureTtlSeconds())));
		return new UploadIntent(saved.getId(), storageProvider.providerType(), instructions);
	}

	@Transactional(readOnly = true)
	public List<PropertyMedia> listReadyMedia(Long organizationId, Long propertyId) {
		authorizeOwner(organizationId);
		requireProperty(organizationId, propertyId);
		return mediaRepository.findByOrganizationIdAndPropertyIdAndStatusOrderByPositionAsc(
				organizationId, propertyId, PropertyMediaStatus.READY);
	}

	public PropertyMedia completeUpload(
			Long organizationId,
			Long propertyId,
			Long mediaId,
			CompleteUploadRequest request) {
		authorizeOwner(organizationId);
		requireProperty(organizationId, propertyId);
		PropertyMedia media = mediaRepository.findByOrganizationIdAndPropertyIdAndIdWithLock(
				organizationId, propertyId, mediaId)
				.orElseThrow(() -> new IllegalArgumentException("Property media was not found"));
		if (media.getStatus() == PropertyMediaStatus.READY) {
			return media;
		}
		if (media.getStatus() != PropertyMediaStatus.PENDING) {
			throw new IllegalStateException("Only pending media can be completed");
		}
		if (media.isUploadExpired(Instant.now())) {
			media.markFailed();
			mediaRepository.saveAndFlush(media);
			try {
				storageProvider.delete(new MediaStorageProvider.DeleteRequest(
					null,
						new MediaStorageProvider.StorageKey(media.getProviderStorageKey())));
			} catch (RuntimeException cleanupException) {
				LOGGER.warn("Unable to clean up expired property media {}", media.getId(), cleanupException);
			}
			throw new IllegalStateException("The media upload intent has expired");
		}
		try {
			MediaStorageProvider.StoredAssetMetadata verified = storageProvider.verifyUploadedAsset(
					new MediaStorageProvider.AssetVerificationRequest(
							new MediaStorageProvider.StorageKey(media.getProviderStorageKey()),
							request.providerAssetId(),
							request.providerAssetUrl(),
							media.getMimeType(),
							media.getByteSize(),
							media.getUploadChecksumSha256()));
			media.markReady(
					verified.providerAssetId(),
					verified.secureUrl(),
					verified.checksumSha256(),
					verified.width(),
					verified.height());
			return mediaRepository.saveAndFlush(media);
		} catch (RuntimeException exception) {
			media.markFailed();
			mediaRepository.saveAndFlush(media);
			try {
				storageProvider.delete(new MediaStorageProvider.DeleteRequest(
						null,
						new MediaStorageProvider.StorageKey(media.getProviderStorageKey())));
			} catch (RuntimeException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw exception;
		}
	}

	public PropertyMedia selectPrimary(Long organizationId, Long propertyId, Long mediaId) {
		authorizeOwner(organizationId);
		requireProperty(organizationId, propertyId);
		List<PropertyMedia> media = lockedReadyMedia(organizationId, propertyId);
		PropertyMedia selected = media.stream()
				.filter(item -> item.getId().equals(mediaId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("READY property media was not found"));
		media.forEach(PropertyMedia::clearPrimary);
		selected.selectAsPrimary();
		mediaRepository.saveAllAndFlush(media);
		return selected;
	}

	public List<PropertyMedia> reorder(
			Long organizationId,
			Long propertyId,
			List<Long> mediaIds) {
		authorizeOwner(organizationId);
		requireProperty(organizationId, propertyId);
		if (mediaIds == null || mediaIds.isEmpty()) {
			throw new IllegalArgumentException("Media order must not be empty");
		}
		List<PropertyMedia> media = lockedReadyMedia(organizationId, propertyId);
		Set<Long> expectedIds = media.stream().map(PropertyMedia::getId).collect(java.util.stream.Collectors.toSet());
		Set<Long> requestedIds = new HashSet<>(mediaIds);
		if (requestedIds.size() != mediaIds.size() || !expectedIds.equals(requestedIds)) {
			throw new IllegalArgumentException("Media order must contain each READY image exactly once");
		}
		Map<Long, PropertyMedia> byId = new HashMap<>();
		media.forEach(item -> byId.put(item.getId(), item));
		for (int index = 0; index < mediaIds.size(); index++) {
			byId.get(mediaIds.get(index)).updatePosition(index);
		}
		return mediaRepository.saveAllAndFlush(media);
	}

	public void delete(Long organizationId, Long propertyId, Long mediaId) {
		authorizeOwner(organizationId);
		requireProperty(organizationId, propertyId);
		PropertyMedia media = mediaRepository.findByOrganizationIdAndPropertyIdAndIdWithLock(
				organizationId, propertyId, mediaId)
				.orElseThrow(() -> new IllegalArgumentException("Property media was not found"));
		if (media.getStatus() == PropertyMediaStatus.DELETED) {
			return;
		}
		boolean wasPrimary = media.isPrimaryImage();
		media.markDeleting();
		mediaRepository.saveAndFlush(media);
		storageProvider.delete(new MediaStorageProvider.DeleteRequest(
				media.getProviderAssetId(),
				new MediaStorageProvider.StorageKey(media.getProviderStorageKey())));
		media.markDeleted();
		mediaRepository.saveAndFlush(media);
		if (wasPrimary) {
			List<PropertyMedia> remaining = lockedReadyMedia(organizationId, propertyId);
			if (!remaining.isEmpty()) {
				remaining.forEach(PropertyMedia::clearPrimary);
				remaining.get(0).selectAsPrimary();
				mediaRepository.saveAllAndFlush(remaining);
			}
		}
	}

	private List<PropertyMedia> lockedReadyMedia(Long organizationId, Long propertyId) {
		return new ArrayList<>(mediaRepository.findByOrganizationIdAndPropertyIdAndStatusWithLock(
				organizationId, propertyId, PropertyMediaStatus.READY));
	}

	private Organization authorizeOwner(Long organizationId) {
		return accessPolicy.requireOwnerOrAdmin(organizationId, accessPolicy.currentUserId());
	}

	private Property requireProperty(Long organizationId, Long propertyId) {
		return propertyRepository.findByOrganizationIdAndId(organizationId, propertyId)
				.orElseThrow(() -> new IllegalArgumentException("Property was not found"));
	}

	private void validateUploadRequest(CreateUploadIntentRequest request) {
		if (request == null || request.mimeType() == null || request.mimeType().isBlank()) {
			throw new IllegalArgumentException("Image MIME type is required");
		}
		if (request.byteSize() <= 0 || request.byteSize() > storageProperties.maxBytesPerImage()) {
			throw new IllegalArgumentException("Image size is outside the allowed limit");
		}
		if (!storageProperties.allowedFormatList().contains(formatFromMimeType(request.mimeType()))) {
			throw new IllegalArgumentException("Image format is not allowed");
		}
		if (request.checksumSha256() != null
				&& !request.checksumSha256().matches("^[0-9a-fA-F]{64}$")) {
			throw new IllegalArgumentException("Image checksum must be a SHA-256 value");
		}
	}

	private static String formatFromMimeType(String mimeType) {
		return switch (mimeType.toLowerCase()) {
			case "image/jpeg" -> "jpg";
			case "image/png" -> "png";
			case "image/webp" -> "webp";
			default -> throw new IllegalArgumentException("Unsupported image MIME type");
		};
	}

	public record CreateUploadIntentRequest(
			String mimeType,
			long byteSize,
			String checksumSha256) {
	}

	public record CompleteUploadRequest(
			String providerAssetId,
			String providerAssetUrl,
			String checksumSha256) {
	}

	public record UploadIntent(
			Long mediaId,
			String providerType,
			MediaStorageProvider.UploadInstructions instructions) {
	}
}
