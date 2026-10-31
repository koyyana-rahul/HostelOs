package com.rahul.HostelOs.inventory;

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
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "properties")
public class Property {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false, updatable = false)
	private Organization organization;

	@Column(name = "name", nullable = false, length = 120)
	private String name;

	@Column(name = "code", nullable = false, length = 30, updatable = false)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private PropertyStatus status;

	@Column(name = "address_line1", length = 200)
	private String addressLine1;

	@Column(name = "address_line2", length = 200)
	private String addressLine2;

	@Column(name = "city", nullable = false, length = 100)
	private String city;

	@Column(name = "state", length = 100)
	private String state;

	@Column(name = "postal_code", length = 20)
	private String postalCode;

	@Column(name = "country", nullable = false, length = 100)
	private String country;

	@Column(name = "timezone", nullable = false, length = 50)
	private String timezone;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Property() {
	}

	public Property(
			Organization organization,
			String name,
			String rawCode,
			PropertyStatus status,
			String addressLine1,
			String addressLine2,
			String city,
			String state,
			String postalCode,
			String country,
			String timezone) {
		this.organization = Objects.requireNonNull(organization, "Organization must not be null");
		this.name = validateName(name);
		this.code = PropertyCode.normalize(rawCode);
		this.status = Objects.requireNonNull(status, "Property status must not be null");
		this.addressLine1 = normalizeOptional(addressLine1, 200, "Address line 1");
		this.addressLine2 = normalizeOptional(addressLine2, 200, "Address line 2");
		this.city = validateCity(city);
		this.state = normalizeOptional(state, 100, "State");
		this.postalCode = normalizeOptional(postalCode, 20, "Postal code");
		this.country = normalizeCountry(country);
		this.timezone = validateTimezone(timezone);
	}

	@PrePersist
	protected void onPrePersist() {
		this.name = validateName(this.name);
		this.code = PropertyCode.normalize(this.code);
		this.city = validateCity(this.city);
		this.country = normalizeCountry(this.country);
		this.timezone = validateTimezone(this.timezone);
	}

	public Long getId() {
		return id;
	}

	public Organization getOrganization() {
		return organization;
	}

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}

	public PropertyStatus getStatus() {
		return status;
	}

	public String getAddressLine1() {
		return addressLine1;
	}

	public String getAddressLine2() {
		return addressLine2;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCountry() {
		return country;
	}

	public String getTimezone() {
		return timezone;
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

	public void suspend() {
		if (this.status == PropertyStatus.ARCHIVED) {
			throw new IllegalStateException("Cannot suspend an archived property");
		}
		this.status = PropertyStatus.INACTIVE;
	}

	public void activate() {
		if (this.status == PropertyStatus.ARCHIVED) {
			throw new IllegalStateException("Cannot activate an archived property");
		}
		this.status = PropertyStatus.ACTIVE;
	}

	public void archive() {
		this.status = PropertyStatus.ARCHIVED;
	}

	public void updateDetails(
			String newName,
			String newAddressLine1,
			String newAddressLine2,
			String newCity,
			String newState,
			String newPostalCode,
			String newCountry,
			String newTimezone) {
		if (this.status == PropertyStatus.ARCHIVED) {
			throw new IllegalStateException("Cannot update details of an archived property");
		}
		this.name = validateName(newName);
		this.addressLine1 = normalizeOptional(newAddressLine1, 200, "Address line 1");
		this.addressLine2 = normalizeOptional(newAddressLine2, 200, "Address line 2");
		this.city = validateCity(newCity);
		this.state = normalizeOptional(newState, 100, "State");
		this.postalCode = normalizeOptional(newPostalCode, 20, "Postal code");
		this.country = normalizeCountry(newCountry);
		this.timezone = validateTimezone(newTimezone);
	}

	private static String validateName(String name) {
		if (name == null || name.trim().isBlank()) {
			throw new IllegalArgumentException("Property name must not be blank");
		}
		String trimmed = name.trim();
		if (trimmed.length() > 120) {
			throw new IllegalArgumentException("Property name must not exceed 120 characters");
		}
		return trimmed;
	}

	private static String validateCity(String city) {
		if (city == null || city.trim().isBlank()) {
			throw new IllegalArgumentException("Property city must not be blank");
		}
		String trimmed = city.trim();
		if (trimmed.length() > 100) {
			throw new IllegalArgumentException("Property city must not exceed 100 characters");
		}
		return trimmed;
	}

	private static String normalizeCountry(String country) {
		if (country == null || country.trim().isBlank()) {
			return "IN";
		}
		String trimmed = country.trim();
		if (trimmed.length() > 100) {
			throw new IllegalArgumentException("Property country must not exceed 100 characters");
		}
		return trimmed;
	}

	private static String validateTimezone(String timezone) {
		String tz = (timezone == null || timezone.trim().isBlank()) ? "Asia/Kolkata" : timezone.trim();
		if (tz.length() > 50) {
			throw new IllegalArgumentException("Property timezone must not exceed 50 characters");
		}
		try {
			ZoneId.of(tz);
			return tz;
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid property timezone: " + tz, ex);
		}
	}

	private static String normalizeOptional(String value, int maxLength, String fieldName) {
		if (value == null || value.trim().isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return trimmed;
	}
}
