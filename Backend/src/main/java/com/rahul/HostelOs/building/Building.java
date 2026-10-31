package com.rahul.HostelOs.building;

import com.rahul.HostelOs.inventory.Property;
import com.rahul.HostelOs.inventory.PropertyStatus;
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
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "buildings")
public class Building {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "property_id", nullable = false, updatable = false)
	private Property property;

	@Column(name = "name", nullable = false, length = 120)
	private String name;

	@Column(name = "code", nullable = false, length = 30, updatable = false)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private BuildingStatus status;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Building() {
	}

	public Building(Property property, String rawCode, String name) {
		this.property = Objects.requireNonNull(property, "Property must not be null");
		if (property.getStatus() == PropertyStatus.ARCHIVED) {
			throw new IllegalArgumentException("Cannot create a building under an archived property");
		}
		this.code = BuildingCode.normalizeAndValidate(rawCode);
		this.name = validateName(name);
		this.status = BuildingStatus.ACTIVE;
	}

	@PrePersist
	protected void onPrePersist() {
		this.code = BuildingCode.normalize(this.code);
		this.name = validateName(this.name);
	}

	// -------------------------------------------------------------------------
	// State transitions
	// -------------------------------------------------------------------------

	public void archive() {
		if (this.status == BuildingStatus.ARCHIVED) {
			throw new IllegalStateException("Building is already archived");
		}
		this.status = BuildingStatus.ARCHIVED;
	}

	public void reactivate() {
		if (this.status == BuildingStatus.ACTIVE) {
			throw new IllegalStateException("Building is already active");
		}
		this.status = BuildingStatus.ACTIVE;
	}

	public void updateDetails(String newName) {
		if (this.status == BuildingStatus.ARCHIVED) {
			throw new IllegalStateException("Cannot update details of an archived building");
		}
		this.name = validateName(newName);
	}

	// -------------------------------------------------------------------------
	// Accessors
	// -------------------------------------------------------------------------

	public Long getId() {
		return id;
	}

	public Property getProperty() {
		return property;
	}

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}

	public BuildingStatus getStatus() {
		return status;
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

	// -------------------------------------------------------------------------
	// Validation helpers
	// -------------------------------------------------------------------------

	private static String validateName(String name) {
		if (name == null || name.trim().isBlank()) {
			throw new IllegalArgumentException("Building name must not be blank");
		}
		String trimmed = name.trim();
		if (trimmed.length() > 120) {
			throw new IllegalArgumentException("Building name must not exceed 120 characters");
		}
		return trimmed;
	}
}
