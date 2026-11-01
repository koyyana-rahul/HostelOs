package com.rahul.HostelOs.propertymedia;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class CloudinaryStorageProvider implements MediaStorageProvider {

	private static final String IMAGE_RESOURCE_TYPE = "image";

	private final Cloudinary cloudinary;
	private final CloudinaryProperties properties;
	private final MediaStorageProperties mediaProperties;
	private final boolean enabled;

	CloudinaryStorageProvider(
			CloudinaryProperties properties,
			MediaStorageProperties mediaProperties,
			boolean enabled) {
		this.properties = Objects.requireNonNull(properties, "Cloudinary properties must not be null");
		this.mediaProperties = Objects.requireNonNull(mediaProperties, "Media storage properties must not be null");
		this.enabled = enabled;
		this.cloudinary = new Cloudinary(Cloudinary.asMap(
				"cloud_name", properties.cloudName(),
				"api_key", properties.apiKey(),
				"api_secret", properties.apiSecret(),
				"timeout", properties.timeoutSeconds()));
	}

	@Override
	public String providerType() {
		return MediaProviderType.CLOUDINARY.name();
	}

	@Override
	public UploadInstructions createUploadInstructions(UploadRequest request) {
		requireEnabled();
		Objects.requireNonNull(request, "Upload request must not be null");
		long timestamp = Instant.now().getEpochSecond();
		Map<String, Object> signedFields = new LinkedHashMap<>();
		signedFields.put("public_id", request.storageKey().value());
		signedFields.put("timestamp", timestamp);

		Map<String, Object> signatureFields = new LinkedHashMap<>(signedFields);
		String signature = cloudinary.apiSignRequest(signatureFields, properties.apiSecret());

		Map<String, String> parameters = new LinkedHashMap<>();
		signedFields.forEach((key, value) -> parameters.put(key, String.valueOf(value)));
		parameters.put("api_key", requireText(properties.apiKey(), "Cloudinary API key"));
		parameters.put("signature", signature);

		Instant expiresAt = Instant.ofEpochSecond(timestamp + mediaProperties.uploadSignatureTtlSeconds());
		String uploadUrl = "https://api.cloudinary.com/v1_1/"
				+ requireText(properties.cloudName(), "Cloudinary cloud name")
				+ "/image/upload";
		return new UploadInstructions("POST", uploadUrl, Map.copyOf(parameters), expiresAt,
				mediaProperties.maxBytesPerImage());
	}

	@Override
	public StoredAssetMetadata verifyUploadedAsset(AssetVerificationRequest request) {
		requireEnabled();
		Objects.requireNonNull(request, "Asset verification request must not be null");
		String publicId = request.expectedStorageKey().value();
		try {
			ApiResponse response = cloudinary.api().resource(
					publicId,
					Cloudinary.asMap("resource_type", IMAGE_RESOURCE_TYPE, "type", "upload"));
			Map<?, ?> asset = response;
			requireEquals(publicId, text(asset, "public_id"), "Cloudinary public ID");
			requireEquals(IMAGE_RESOURCE_TYPE, text(asset, "resource_type"), "Cloudinary resource type");

			long byteSize = number(asset, "bytes");
			if (byteSize != request.expectedByteSize()) {
				throw new PropertyMediaStorageException("Cloudinary asset size does not match the upload intent");
			}
			String secureUrl = text(asset, "secure_url");
			String format = text(asset, "format").toLowerCase();
			String mimeType = mimeTypeFor(format);
			if (!mimeType.equalsIgnoreCase(request.expectedMimeType())) {
				throw new PropertyMediaStorageException("Cloudinary asset format does not match the upload intent");
			}
			String expectedAssetId = request.providerAssetId();
			String actualAssetId = text(asset, "asset_id");
			if (expectedAssetId != null && !expectedAssetId.isBlank()) {
				requireEquals(expectedAssetId, actualAssetId, "Cloudinary asset ID");
			}
			int width = positiveDimension(asset, "width");
			int height = positiveDimension(asset, "height");
			if ((long) width * height > mediaProperties.maxPixelsPerImage()) {
				throw new PropertyMediaStorageException("Cloudinary asset exceeds the pixel limit");
			}
			String actualChecksum = optionalText(asset, "sha256");
			if (request.expectedChecksumSha256() != null) {
				if (actualChecksum == null
						|| !request.expectedChecksumSha256().equalsIgnoreCase(actualChecksum)) {
					throw new PropertyMediaStorageException(
							"Cloudinary asset checksum does not match the upload intent");
				}
			}
			return new StoredAssetMetadata(
					actualAssetId,
					request.expectedStorageKey(),
					secureUrl,
					mimeType,
					format,
					byteSize,
					width,
					height,
					actualChecksum);
		} catch (PropertyMediaStorageException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new PropertyMediaStorageException("Cloudinary asset verification failed", exception);
		}
	}

	@Override
	public void delete(DeleteRequest request) {
		requireEnabled();
		Objects.requireNonNull(request, "Delete request must not be null");
		try {
			Map<?, ?> response = cloudinary.uploader().destroy(
					request.storageKey().value(),
					Cloudinary.asMap("resource_type", IMAGE_RESOURCE_TYPE, "type", "upload"));
			String result = text(response, "result");
			if (!"ok".equalsIgnoreCase(result) && !"not found".equalsIgnoreCase(result)) {
				throw new PropertyMediaStorageException("Cloudinary deletion was not successful");
			}
		} catch (PropertyMediaStorageException exception) {
			throw exception;
		} catch (IOException exception) {
			throw new PropertyMediaStorageException("Cloudinary asset deletion failed", exception);
		}
	}

	@Override
	public DeliveryUrl createDeliveryUrl(DeliveryRequest request) {
		requireEnabled();
		Objects.requireNonNull(request, "Delivery request must not be null");
		String url = cloudinary.url()
				.secure(true)
				.resourceType(IMAGE_RESOURCE_TYPE)
				.type("upload")
				.publicId(request.storageKey().value())
				.generate();
		return new DeliveryUrl(url, request.expiresAt());
	}

	@Override
	public StorageKey createStorageKey(StorageKeyRequest request) {
		Objects.requireNonNull(request, "Storage key request must not be null");
		if (request.organizationId() <= 0 || request.propertyId() <= 0 || request.mediaId() <= 0) {
			throw new IllegalArgumentException("Storage key identifiers must be positive");
		}
		return new StorageKey(properties.folderPrefix()
				+ "/org-" + request.organizationId()
				+ "/property-" + request.propertyId()
				+ "/media-" + request.mediaId());
	}

	private void requireEnabled() {
		if (!enabled) {
			throw new PropertyMediaStorageException("Property media storage is disabled");
		}
	}

	private static String text(Map<?, ?> values, String key) {
		Object value = values.get(key);
		if (value == null || String.valueOf(value).isBlank()) {
			throw new PropertyMediaStorageException("Cloudinary response is missing " + key);
		}
		return String.valueOf(value);
	}

	private static long number(Map<?, ?> values, String key) {
		Object value = values.get(key);
		if (!(value instanceof Number number) || number.longValue() <= 0) {
			throw new PropertyMediaStorageException("Cloudinary response has invalid " + key);
		}
		return number.longValue();
	}

	private static int positiveDimension(Map<?, ?> values, String key) {
		long value = number(values, key);
		if (value > Integer.MAX_VALUE) {
			throw new PropertyMediaStorageException("Cloudinary response has invalid " + key);
		}
		return (int) value;
	}

	private static String optionalText(Map<?, ?> values, String key) {
		Object value = values.get(key);
		return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
	}

	private static void requireEquals(String expected, String actual, String fieldName) {
		if (!expected.equals(actual)) {
			throw new PropertyMediaStorageException(fieldName + " does not match the upload intent");
		}
	}

	private static String mimeTypeFor(String format) {
		return switch (format) {
			case "jpg", "jpeg" -> "image/jpeg";
			case "png" -> "image/png";
			case "webp" -> "image/webp";
			default -> throw new PropertyMediaStorageException("Unsupported Cloudinary image format");
		};
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(fieldName + " must be configured");
		}
		return value;
	}
}
