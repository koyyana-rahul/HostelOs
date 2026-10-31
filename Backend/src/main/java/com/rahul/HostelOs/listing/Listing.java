package com.rahul.HostelOs.listing;

import com.rahul.HostelOs.inventory.Property;
import com.rahul.HostelOs.listing.exception.InvalidListingStatusTransitionException;
import com.rahul.HostelOs.listing.exception.ListingValidationException;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "listings")
public class Listing {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false, updatable = false)
	private Organization organization;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "property_id")
	private Property property;

	@Column(name = "title", nullable = false, length = 160)
	private String title;

	@Column(name = "title_key", nullable = false, length = 160)
	private String titleKey;

	@Column(name = "description", length = 4000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "accommodation_type", nullable = false, length = 30)
	private AccommodationType accommodationType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ListingStatus status;

	@Column(name = "address_line1", length = 200)
	private String addressLine1;

	@Column(name = "city", length = 100)
	private String city;

	@Column(name = "state", length = 100)
	private String state;

	@Column(name = "postal_code", length = 20)
	private String postalCode;

	@Column(name = "country", length = 100)
	private String country;

	@Column(name = "latitude", precision = 9, scale = 6)
	private BigDecimal latitude;

	@Column(name = "longitude", precision = 9, scale = 6)
	private BigDecimal longitude;

	@Column(name = "base_price", precision = 10, scale = 2)
	private BigDecimal basePrice;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "price_period", nullable = false, length = 20)
	private PricePeriod pricePeriod;

	@Column(name = "contact_name", length = 120)
	private String contactName;

	@Column(name = "contact_email", length = 254)
	private String contactEmail;

	@Column(name = "contact_phone", length = 40)
	private String contactPhone;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "archived_at")
	private LocalDateTime archivedAt;

	protected Listing() {
	}

	public Listing(
			Organization organization,
			Property property,
			String title,
			String description,
			AccommodationType accommodationType,
			String addressLine1,
			String city,
			String state,
			String postalCode,
			String country,
			BigDecimal latitude,
			BigDecimal longitude,
			BigDecimal basePrice,
			String currency,
			PricePeriod pricePeriod,
			String contactName,
			String contactEmail,
			String contactPhone) {
		this.organization = Objects.requireNonNull(organization, "Organization must not be null");
		this.status = ListingStatus.DRAFT;
		this.property = property;
		applyDetails(
				title,
				description,
				accommodationType,
				addressLine1,
				city,
				state,
				postalCode,
				country,
				latitude,
				longitude,
				basePrice,
				currency,
				pricePeriod,
				contactName,
				contactEmail,
				contactPhone);
	}

	public void updateDetails(
			Property newProperty,
			String newTitle,
			String newDescription,
			AccommodationType newAccommodationType,
			String newAddressLine1,
			String newCity,
			String newState,
			String newPostalCode,
			String newCountry,
			BigDecimal newLatitude,
			BigDecimal newLongitude,
			BigDecimal newBasePrice,
			String newCurrency,
			PricePeriod newPricePeriod,
			String newContactName,
			String newContactEmail,
			String newContactPhone) {
		if (this.status == ListingStatus.ARCHIVED) {
			throw new InvalidListingStatusTransitionException("Archived listings cannot be edited");
		}
		this.property = newProperty;
		applyDetails(
				newTitle,
				newDescription,
				newAccommodationType,
				newAddressLine1,
				newCity,
				newState,
				newPostalCode,
				newCountry,
				newLatitude,
				newLongitude,
				newBasePrice,
				newCurrency,
				newPricePeriod,
				newContactName,
				newContactEmail,
				newContactPhone);
	}

	public void publish() {
		if (this.status == ListingStatus.ARCHIVED) {
			throw new InvalidListingStatusTransitionException("Archived listings cannot be published");
		}
		if (this.status == ListingStatus.PUBLISHED) {
			throw new InvalidListingStatusTransitionException("Listing is already published");
		}
		if (this.description == null || this.description.isBlank()) {
			throw new ListingValidationException("Description is required before publishing");
		}
		if (this.city == null || this.city.isBlank()) {
			throw new ListingValidationException("City is required before publishing");
		}
		if (this.latitude == null || this.longitude == null) {
			throw new ListingValidationException("Latitude and longitude are required before publishing");
		}
		if (this.basePrice == null) {
			throw new ListingValidationException("Base price is required before publishing");
		}
		this.status = ListingStatus.PUBLISHED;
	}

	public void unpublish() {
		if (this.status == ListingStatus.ARCHIVED) {
			throw new InvalidListingStatusTransitionException("Archived listings cannot be unpublished");
		}
		if (this.status != ListingStatus.PUBLISHED) {
			throw new InvalidListingStatusTransitionException("Only published listings can be unpublished");
		}
		this.status = ListingStatus.DRAFT;
	}

	public void archive() {
		if (this.status == ListingStatus.ARCHIVED) {
			throw new InvalidListingStatusTransitionException("Listing is already archived");
		}
		this.status = ListingStatus.ARCHIVED;
		this.archivedAt = LocalDateTime.now();
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

	public String getTitle() {
		return title;
	}

	public String getTitleKey() {
		return titleKey;
	}

	public String getDescription() {
		return description;
	}

	public AccommodationType getAccommodationType() {
		return accommodationType;
	}

	public ListingStatus getStatus() {
		return status;
	}

	public String getAddressLine1() {
		return addressLine1;
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

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public BigDecimal getBasePrice() {
		return basePrice;
	}

	public String getCurrency() {
		return currency;
	}

	public PricePeriod getPricePeriod() {
		return pricePeriod;
	}

	public String getContactName() {
		return contactName;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public String getContactPhone() {
		return contactPhone;
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

	public LocalDateTime getArchivedAt() {
		return archivedAt;
	}

	private void applyDetails(
			String rawTitle,
			String rawDescription,
			AccommodationType newAccommodationType,
			String newAddressLine1,
			String newCity,
			String newState,
			String newPostalCode,
			String newCountry,
			BigDecimal newLatitude,
			BigDecimal newLongitude,
			BigDecimal newBasePrice,
			String newCurrency,
			PricePeriod newPricePeriod,
			String newContactName,
			String newContactEmail,
			String newContactPhone) {
		String normalizedTitle = normalizeTitle(rawTitle);
		this.title = normalizedTitle;
		this.titleKey = normalizedTitle.toLowerCase(Locale.ROOT);
		this.description = normalizeOptional(rawDescription, 4000, "Description");
		this.accommodationType = newAccommodationType == null ? AccommodationType.OTHER : newAccommodationType;
		this.addressLine1 = normalizeOptional(newAddressLine1, 200, "Address line 1");
		this.city = normalizeOptional(newCity, 100, "City");
		this.state = normalizeOptional(newState, 100, "State");
		this.postalCode = normalizeOptional(newPostalCode, 20, "Postal code");
		this.country = normalizeOptional(newCountry, 100, "Country");
		this.latitude = validateLatitude(newLatitude);
		this.longitude = validateLongitude(newLongitude);
		this.basePrice = validateBasePrice(newBasePrice);
		this.currency = normalizeCurrency(newCurrency);
		this.pricePeriod = newPricePeriod == null ? PricePeriod.MONTH : newPricePeriod;
		this.contactName = normalizeOptional(newContactName, 120, "Contact name");
		this.contactEmail = normalizeOptional(newContactEmail, 254, "Contact email");
		this.contactPhone = normalizeOptional(newContactPhone, 40, "Contact phone");
	}

	private static String normalizeTitle(String title) {
		if (title == null || title.isBlank()) {
			throw new ListingValidationException("Listing title must not be blank");
		}
		String normalized = title.trim().replaceAll("\\s+", " ");
		if (normalized.length() < 3 || normalized.length() > 160) {
			throw new ListingValidationException("Listing title must be between 3 and 160 characters");
		}
		return normalized;
	}

	private static String normalizeOptional(String value, int maxLength, String fieldName) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new ListingValidationException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return trimmed;
	}

	private static BigDecimal validateLatitude(BigDecimal latitude) {
		if (latitude == null) {
			return null;
		}
		if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
			throw new ListingValidationException("Latitude must be between -90 and 90");
		}
		return latitude;
	}

	private static BigDecimal validateLongitude(BigDecimal longitude) {
		if (longitude == null) {
			return null;
		}
		if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
			throw new ListingValidationException("Longitude must be between -180 and 180");
		}
		return longitude;
	}

	private static BigDecimal validateBasePrice(BigDecimal basePrice) {
		if (basePrice == null) {
			return null;
		}
		if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
			throw new ListingValidationException("Base price must not be negative");
		}
		return basePrice;
	}

	private static String normalizeCurrency(String currency) {
		String normalized = (currency == null || currency.isBlank()) ? "INR" : currency.trim().toUpperCase(Locale.ROOT);
		if (normalized.length() != 3) {
			throw new ListingValidationException("Currency must be a three-letter code");
		}
		return normalized;
	}
}
