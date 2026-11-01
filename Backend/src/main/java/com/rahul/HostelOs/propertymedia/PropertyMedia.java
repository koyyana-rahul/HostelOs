package com.rahul.HostelOs.propertymedia;

import com.rahul.HostelOs.inventory.Property;
import com.rahul.HostelOs.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "property_media")
public class PropertyMedia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false, updatable = false)
	private Organization organization;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "property_id", nullable = false, updatable = false)
	private Property property;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider_type", nullable = false, length = 30)
	private MediaProviderType providerType;

	@Column(name = "provider_storage_key", nullable = false, length = 512)
	private String providerStorageKey;

	@Column(name = "provider_asset_id", length = 255)
	private String providerAssetId;

	@Column(name = "mime_type", nullable = false, length = 100)
	private String mimeType;

	@Column(name = "format", nullable = false, length = 20)
	private String format;

	@Column(name = "secure_url", length = 1000)
	private String secureUrl;

	@Column(name = "byte_size", nullable = false)
	private long byteSize;

	@Column(name = "width")
	private Integer width;

	@Column(name = "height")
	private Integer height;

	@Column(name = "checksum_sha256", length = 64)
	private String checksumSha256;

	@Column(name = "upload_checksum_sha256", length = 64)
	private String uploadChecksumSha256;

	@Column(name = "position", nullable = false)
	private int position;

	@Column(name = "is_primary", nullable = false)
	private boolean primaryImage;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PropertyMediaStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "migration_status", nullable = false, length = 30)
	private MediaMigrationStatus migrationStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "migration_source_provider", length = 30)
	private MediaProviderType migrationSourceProvider;

	@Enumerated(EnumType.STRING)
	@Column(name = "migration_target_provider", length = 30)
	private MediaProviderType migrationTargetProvider;

	@Column(name = "migration_source_key", length = 512)
	private String migrationSourceKey;

	@Column(name = "migration_target_key", length = 512)
	private String migrationTargetKey;

	@Column(name = "migration_checksum_sha256", length = 64)
	private String migrationChecksumSha256;

	@Column(name = "migration_idempotency_key", length = 128)
	private String migrationIdempotencyKey;

	@Column(name = "migration_retry_count", nullable = false)
	private int migrationRetryCount;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "upload_expires_at")
	private LocalDateTime uploadExpiresAt;

	protected PropertyMedia() {
	}

	public PropertyMedia(
			Organization organization,
			Property property,
			MediaProviderType providerType,
			String providerStorageKey,
			String mimeType,
			String format,
			long byteSize,
			int position) {
		this(organization, property, providerType, providerStorageKey, mimeType, format, byteSize, position, null);
	}

	public PropertyMedia(
			Organization organization,
			Property property,
			MediaProviderType providerType,
			String providerStorageKey,
			String mimeType,
			String format,
			long byteSize,
			int position,
			Instant uploadExpiresAt) {
		this(organization, property, providerType, providerStorageKey, mimeType, format, byteSize,
				position, uploadExpiresAt, null);
	}

	public PropertyMedia(
			Organization organization,
			Property property,
			MediaProviderType providerType,
			String providerStorageKey,
			String mimeType,
			String format,
			long byteSize,
			int position,
			Instant uploadExpiresAt,
			String uploadChecksumSha256) {
		this.organization = Objects.requireNonNull(organization, "Organization must not be null");
		this.property = Objects.requireNonNull(property, "Property must not be null");
		if (!organization.getId().equals(property.getOrganization().getId())) {
			throw new IllegalArgumentException("Property must belong to the organization");
		}
		this.providerType = Objects.requireNonNull(providerType, "Provider type must not be null");
		this.providerStorageKey = requireText(providerStorageKey, "Provider storage key");
		this.mimeType = requireText(mimeType, "MIME type");
		this.format = requireText(format, "Format");
		if (byteSize <= 0) {
			throw new IllegalArgumentException("Byte size must be positive");
		}
		if (position < 0) {
			throw new IllegalArgumentException("Position must not be negative");
		}
		this.byteSize = byteSize;
		this.position = position;
		this.primaryImage = false;
		this.status = PropertyMediaStatus.PENDING;
		this.migrationStatus = MediaMigrationStatus.NOT_STARTED;
		this.migrationRetryCount = 0;
		this.uploadExpiresAt = uploadExpiresAt == null ? null : LocalDateTime.ofInstant(uploadExpiresAt, java.time.ZoneOffset.UTC);
		this.uploadChecksumSha256 = uploadChecksumSha256;
	}

	@PrePersist
	protected void onPrePersist() {
		if (this.status == null) {
			this.status = PropertyMediaStatus.PENDING;
		}
		if (this.migrationStatus == null) {
			this.migrationStatus = MediaMigrationStatus.NOT_STARTED;
		}
	}

	public Long getId() {
		return id;
	}

	public Organization getOrganization() {
		return organization;
	}

	public Property getProperty() {
		return property;
	}

	public MediaProviderType getProviderType() {
		return providerType;
	}

	public String getProviderStorageKey() {
		return providerStorageKey;
	}

	public String getProviderAssetId() {
		return providerAssetId;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getFormat() {
		return format;
	}

	public String getSecureUrl() {
		return secureUrl;
	}

	public long getByteSize() {
		return byteSize;
	}

	public Integer getWidth() {
		return width;
	}

	public Integer getHeight() {
		return height;
	}

	public String getChecksumSha256() {
		return checksumSha256;
	}

	public String getUploadChecksumSha256() {
		return uploadChecksumSha256;
	}

	public int getPosition() {
		return position;
	}

	public boolean isPrimaryImage() {
		return primaryImage;
	}

	public PropertyMediaStatus getStatus() {
		return status;
	}

	public MediaMigrationStatus getMigrationStatus() {
		return migrationStatus;
	}

	public MediaProviderType getMigrationSourceProvider() {
		return migrationSourceProvider;
	}

	public MediaProviderType getMigrationTargetProvider() {
		return migrationTargetProvider;
	}

	public String getMigrationSourceKey() {
		return migrationSourceKey;
	}

	public String getMigrationTargetKey() {
		return migrationTargetKey;
	}

	public String getMigrationChecksumSha256() {
		return migrationChecksumSha256;
	}

	public String getMigrationIdempotencyKey() {
		return migrationIdempotencyKey;
	}

	public int getMigrationRetryCount() {
		return migrationRetryCount;
	}

	public long getVersion() {
		return version;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public LocalDateTime getUploadExpiresAt() {
		return uploadExpiresAt;
	}

	public boolean isUploadExpired(Instant now) {
		return uploadExpiresAt != null
				&& !uploadExpiresAt.isAfter(LocalDateTime.ofInstant(now, java.time.ZoneOffset.UTC));
	}

	public void markReady(
			String providerAssetId,
			String secureUrl,
			String checksumSha256,
			int width,
			int height) {
		this.providerAssetId = requireText(providerAssetId, "Provider asset ID");
		this.secureUrl = requireText(secureUrl, "Secure URL");
		this.checksumSha256 = checksumSha256;
		this.width = requirePositiveDimension(width, "Width");
		this.height = requirePositiveDimension(height, "Height");
		this.status = PropertyMediaStatus.READY;
	}

	public void replaceProviderStorageKey(String newProviderStorageKey) {
		if (this.status != PropertyMediaStatus.PENDING) {
			throw new IllegalStateException("Only pending media can change its storage key");
		}
		this.providerStorageKey = requireText(newProviderStorageKey, "Provider storage key");
	}

	public void markFailed() {
		this.status = PropertyMediaStatus.FAILED;
		this.primaryImage = false;
	}

	public void markDeleting() {
		this.status = PropertyMediaStatus.DELETING;
		this.primaryImage = false;
	}

	public void markDeleted() {
		this.status = PropertyMediaStatus.DELETED;
		this.primaryImage = false;
	}

	public void selectAsPrimary() {
		if (this.status != PropertyMediaStatus.READY) {
			throw new IllegalStateException("Only READY media can be primary");
		}
		this.primaryImage = true;
	}

	public void clearPrimary() {
		this.primaryImage = false;
	}

	public void updatePosition(int newPosition) {
		if (newPosition < 0) {
			throw new IllegalArgumentException("Position must not be negative");
		}
		this.position = newPosition;
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}

	private static int requirePositiveDimension(int value, String fieldName) {
		if (value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return value;
	}
}
