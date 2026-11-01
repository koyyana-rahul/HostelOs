package com.rahul.HostelOs.propertymedia;

import java.time.Instant;
import java.util.Map;

/**
 * Provider-neutral boundary for property media storage and delivery.
 *
 * <p>Implementations may use different provider-specific identifiers internally, but callers
 * must use the application media ID and the provider-neutral records declared here.</p>
 */
public interface MediaStorageProvider {

	String providerType();

	UploadInstructions createUploadInstructions(UploadRequest request);

	StoredAssetMetadata verifyUploadedAsset(AssetVerificationRequest request);

	void delete(DeleteRequest request);

	DeliveryUrl createDeliveryUrl(DeliveryRequest request);

	StorageKey createStorageKey(StorageKeyRequest request);

	record UploadRequest(
			StorageKey storageKey,
			String mimeType,
			long byteSize,
			String checksumSha256,
			Instant expiresAt) {
	}

	record UploadInstructions(
			String method,
			String uploadUrl,
			Map<String, String> parameters,
			Instant expiresAt,
			long maxBytes) {
	}

	record AssetVerificationRequest(
			StorageKey expectedStorageKey,
			String providerAssetId,
			String providerAssetUrl,
			String expectedMimeType,
			long expectedByteSize,
			String expectedChecksumSha256) {
	}

	record StoredAssetMetadata(
			String providerAssetId,
			StorageKey storageKey,
			String secureUrl,
			String mimeType,
			String format,
			long byteSize,
			int width,
			int height,
			String checksumSha256) {
	}

	record DeleteRequest(String providerAssetId, StorageKey storageKey) {
	}

	record DeliveryRequest(String providerAssetId, StorageKey storageKey, Instant expiresAt) {
	}

	record DeliveryUrl(String url, Instant expiresAt) {
	}

	record StorageKeyRequest(long organizationId, long propertyId, long mediaId) {
	}

	record StorageKey(String value) {
		public StorageKey {
			if (value == null || value.isBlank()) {
				throw new IllegalArgumentException("Storage key must not be blank");
			}
		}
	}
}
