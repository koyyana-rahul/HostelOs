package com.rahul.HostelOs.enquiry;

import com.rahul.HostelOs.enquiry.exception.InvalidEnquiryStatusTransitionException;
import com.rahul.HostelOs.listing.Listing;
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
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "listing_enquiries")
public class Enquiry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organization_id", nullable = false, updatable = false)
	private Organization organization;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "listing_id", nullable = false, updatable = false)
	private Listing listing;

	@Column(name = "customer_user_id")
	private Long customerUserId;

	@Column(name = "customer_name", nullable = false, length = 120)
	private String customerName;

	@Column(name = "customer_email", nullable = false, length = 254)
	private String customerEmail;

	@Column(name = "customer_phone", length = 40)
	private String customerPhone;

	@Column(name = "message", nullable = false, length = 2000)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private EnquiryStatus status;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Enquiry() {
	}

	public Enquiry(
			Listing listing,
			Long customerUserId,
			String customerName,
			String customerEmail,
			String customerPhone,
			String message) {
		this.listing = Objects.requireNonNull(listing, "Listing must not be null");
		this.organization = listing.getOrganization();
		this.customerUserId = customerUserId;
		this.customerName = normalizeRequired(customerName, 120, "Customer name");
		this.customerEmail = normalizeRequired(customerEmail, 254, "Customer email");
		this.customerPhone = normalizeOptional(customerPhone, 40, "Customer phone");
		this.message = normalizeRequired(message, 2000, "Message");
		this.status = EnquiryStatus.NEW;
	}

	public void changeStatus(EnquiryStatus targetStatus) {
		if (targetStatus == null) {
			throw new InvalidEnquiryStatusTransitionException("Enquiry status is required");
		}
		if (this.status == EnquiryStatus.CLOSED) {
			throw new InvalidEnquiryStatusTransitionException("Closed enquiries cannot be changed");
		}
		if (this.status == targetStatus) {
			throw new InvalidEnquiryStatusTransitionException("Enquiry already has status " + targetStatus);
		}
		if (targetStatus == EnquiryStatus.NEW) {
			throw new InvalidEnquiryStatusTransitionException("Enquiries cannot be moved back to NEW");
		}
		if (targetStatus == EnquiryStatus.RESPONDED && this.status == EnquiryStatus.RESPONDED) {
			throw new InvalidEnquiryStatusTransitionException("Enquiry is already responded");
		}
		this.status = targetStatus;
	}

	public Long getId() {
		return id;
	}

	public Organization getOrganization() {
		return organization;
	}

	public Listing getListing() {
		return listing;
	}

	public Long getCustomerUserId() {
		return customerUserId;
	}

	public String getCustomerName() {
		return customerName;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public String getCustomerPhone() {
		return customerPhone;
	}

	public String getMessage() {
		return message;
	}

	public EnquiryStatus getStatus() {
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

	private static String normalizeRequired(String value, int maxLength, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return trimmed;
	}

	private static String normalizeOptional(String value, int maxLength, String fieldName) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return trimmed;
	}
}
