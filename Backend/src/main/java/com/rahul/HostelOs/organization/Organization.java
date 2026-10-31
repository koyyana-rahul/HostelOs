package com.rahul.HostelOs.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "organizations")
public class Organization {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 120)
	private String name;

	@Column(name = "slug", nullable = false, length = 60, unique = true, updatable = false)
	private String slug;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrganizationStatus status;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Organization() {
	}

	public Organization(String name, String rawSlug, OrganizationStatus status) {
		this.name = validateName(name);
		this.slug = OrganizationSlug.normalize(rawSlug);
		this.status = Objects.requireNonNull(status, "Organization status must not be null");
	}

	@PrePersist
	protected void onPrePersist() {
		this.name = validateName(this.name);
		this.slug = OrganizationSlug.normalize(this.slug);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSlug() {
		return slug;
	}

	public OrganizationStatus getStatus() {
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

	public void updateName(String newName) {
		this.name = validateName(newName);
	}

	public void suspend() {
		this.status = OrganizationStatus.SUSPENDED;
	}

	public void activate() {
		this.status = OrganizationStatus.ACTIVE;
	}

	public void deactivate() {
		this.status = OrganizationStatus.DEACTIVATED;
	}

	private static String validateName(String inputName) {
		if (inputName == null || inputName.trim().isBlank()) {
			throw new IllegalArgumentException("Organization name must not be blank");
		}
		String trimmed = inputName.trim();
		if (trimmed.length() > 120) {
			throw new IllegalArgumentException("Organization name must not exceed 120 characters");
		}
		return trimmed;
	}
}
