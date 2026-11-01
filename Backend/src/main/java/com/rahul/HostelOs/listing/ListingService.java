package com.rahul.HostelOs.listing;

import com.rahul.HostelOs.inventory.Property;
import com.rahul.HostelOs.inventory.PropertyRepository;
import com.rahul.HostelOs.inventory.exception.PropertyNotFoundException;
import com.rahul.HostelOs.listing.dto.ChangeListingStatusRequest;
import com.rahul.HostelOs.listing.dto.CreateListingRequest;
import com.rahul.HostelOs.listing.dto.ListingResponse;
import com.rahul.HostelOs.listing.dto.PublicListingResponse;
import com.rahul.HostelOs.listing.dto.UpdateListingRequest;
import com.rahul.HostelOs.listing.exception.DuplicateListingTitleException;
import com.rahul.HostelOs.listing.exception.InvalidListingStatusTransitionException;
import com.rahul.HostelOs.listing.exception.ListingNotFoundException;
import com.rahul.HostelOs.listing.exception.ListingVersionConflictException;
import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.organization.OrganizationAccessPolicy;
import com.rahul.HostelOs.organization.OrganizationStatus;
import com.rahul.HostelOs.propertymedia.PropertyMedia;
import com.rahul.HostelOs.propertymedia.PropertyMediaRepository;
import com.rahul.HostelOs.propertymedia.PropertyMediaStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ListingService {

	private static final int MAX_PAGE_SIZE = 50;

	private final ListingRepository listingRepository;
	private final PropertyRepository propertyRepository;
	private final OrganizationAccessPolicy accessPolicy;
	private final PropertyMediaRepository propertyMediaRepository;

	public ListingService(
			ListingRepository listingRepository,
			PropertyRepository propertyRepository,
			OrganizationAccessPolicy accessPolicy,
			PropertyMediaRepository propertyMediaRepository) {
		this.listingRepository = Objects.requireNonNull(listingRepository, "ListingRepository must not be null");
		this.propertyRepository = Objects.requireNonNull(propertyRepository, "PropertyRepository must not be null");
		this.accessPolicy = Objects.requireNonNull(accessPolicy, "OrganizationAccessPolicy must not be null");
		this.propertyMediaRepository = Objects.requireNonNull(propertyMediaRepository, "PropertyMediaRepository must not be null");
	}

	public ListingResponse createListing(Long organizationId, CreateListingRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("CreateListingRequest must not be null");
		}
		Long userId = accessPolicy.currentUserId();
		Organization organization = accessPolicy.requireOwnerOrAdmin(organizationId, userId);
		Property property = resolveProperty(organizationId, request.propertyId());
		String titleKey = titleKey(request.title());
		if (listingRepository.existsByOrganizationIdAndTitleKey(organizationId, titleKey)) {
			throw new DuplicateListingTitleException(request.title());
		}
		Listing listing = new Listing(
				organization,
				property,
				request.title(),
				request.description(),
				request.accommodationType(),
				request.addressLine1(),
				request.city(),
				request.state(),
				request.postalCode(),
				request.country(),
				request.latitude(),
				request.longitude(),
				request.basePrice(),
				request.currency(),
				request.pricePeriod(),
				request.contactName(),
				request.contactEmail(),
				request.contactPhone());
		try {
			Listing saved = listingRepository.save(listing);
			listingRepository.flush();
			return ListingResponse.from(saved);
		} catch (DataIntegrityViolationException exception) {
			if (isDuplicateTitleViolation(exception)) {
				throw new DuplicateListingTitleException(request.title());
			}
			throw exception;
		}
	}

	public ListingResponse updateListing(Long organizationId, Long listingId, UpdateListingRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("UpdateListingRequest must not be null");
		}
		Long userId = accessPolicy.currentUserId();
		accessPolicy.requireOwnerOrAdmin(organizationId, userId);
		Listing listing = listingRepository.findByIdAndOrganizationIdWithLock(listingId, organizationId)
				.orElseThrow(() -> new ListingNotFoundException(listingId));
		requireVersion(listing, request.version());
		Property property = resolveProperty(organizationId, request.propertyId());
		String titleKey = titleKey(request.title());
		if (listingRepository.existsByOrganizationIdAndTitleKeyAndIdNot(organizationId, titleKey, listingId)) {
			throw new DuplicateListingTitleException(request.title());
		}
		listing.updateDetails(
				property,
				request.title(),
				request.description(),
				request.accommodationType(),
				request.addressLine1(),
				request.city(),
				request.state(),
				request.postalCode(),
				request.country(),
				request.latitude(),
				request.longitude(),
				request.basePrice(),
				request.currency(),
				request.pricePeriod(),
				request.contactName(),
				request.contactEmail(),
				request.contactPhone());
		try {
			Listing saved = listingRepository.save(listing);
			listingRepository.flush();
			return ListingResponse.from(saved);
		} catch (DataIntegrityViolationException exception) {
			if (isDuplicateTitleViolation(exception)) {
				throw new DuplicateListingTitleException(request.title());
			}
			throw exception;
		}
	}

	public ListingResponse changeStatus(Long organizationId, Long listingId, ChangeListingStatusRequest request) {
		if (request == null || request.status() == null) {
			throw new IllegalArgumentException("Listing status must not be null");
		}
		Long userId = accessPolicy.currentUserId();
		accessPolicy.requireOwnerOrAdmin(organizationId, userId);
		Listing listing = listingRepository.findByIdAndOrganizationIdWithLock(listingId, organizationId)
				.orElseThrow(() -> new ListingNotFoundException(listingId));
		requireVersion(listing, request.version());
		if (request.status() == ListingStatus.PUBLISHED) {
			listing.publish();
		} else if (request.status() == ListingStatus.DRAFT) {
			listing.unpublish();
		} else if (request.status() == ListingStatus.ARCHIVED) {
			listing.archive();
		} else {
			throw new InvalidListingStatusTransitionException("Unsupported listing status: " + request.status());
		}
		Listing saved = listingRepository.save(listing);
		listingRepository.flush();
		return ListingResponse.from(saved);
	}

	public void archiveListing(Long organizationId, Long listingId, Long version) {
		Long userId = accessPolicy.currentUserId();
		accessPolicy.requireOwnerOrAdmin(organizationId, userId);
		Listing listing = listingRepository.findByIdAndOrganizationIdWithLock(listingId, organizationId)
				.orElseThrow(() -> new ListingNotFoundException(listingId));
		requireVersion(listing, version);
		listing.archive();
		listingRepository.saveAndFlush(listing);
	}

	@Transactional(readOnly = true)
	public ListingResponse getListing(Long organizationId, Long listingId) {
		Long userId = accessPolicy.currentUserId();
		accessPolicy.requireMemberAccess(organizationId, userId);
		Listing listing = listingRepository.findByOrganizationIdAndId(organizationId, listingId)
				.orElseThrow(() -> new ListingNotFoundException(listingId));
		return ListingResponse.from(listing);
	}

	@Transactional(readOnly = true)
	public List<ListingResponse> listListings(Long organizationId, ListingStatus status) {
		Long userId = accessPolicy.currentUserId();
		accessPolicy.requireMemberAccess(organizationId, userId);
		List<Listing> listings = status == null
				? listingRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
				: listingRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, status);
		return listings.stream().map(ListingResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public Page<PublicListingResponse> searchPublished(
			String query,
			String city,
			AccommodationType type,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			int page,
			int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
		String normalizedQuery = normalizeSearch(query);
		String normalizedCity = normalizeSearch(city);
		try {
			Page<Listing> listings = listingRepository.searchPublished(
					normalizedQuery, normalizedCity, type, minPrice, maxPrice, pageable);
			return withPublicMedia(listings);
		} catch (RuntimeException exception) {
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public PublicListingResponse getPublishedListing(Long listingId) {
		Listing listing = listingRepository.findByIdAndStatus(listingId, ListingStatus.PUBLISHED)
				.orElseThrow(() -> new ListingNotFoundException(listingId));
		if (listing.getOrganization().getStatus() != OrganizationStatus.ACTIVE) {
			throw new ListingNotFoundException(listingId);
		}
		return PublicListingResponse.from(listing, publicMediaFor(listing));
	}

	private Page<PublicListingResponse> withPublicMedia(Page<Listing> listings) {
		List<Long> propertyIds = listings.getContent().stream()
				.map(Listing::getProperty)
				.filter(Objects::nonNull)
				.map(Property::getId)
				.filter(Objects::nonNull)
				.toList();
		List<PropertyMedia> media = propertyIds.isEmpty()
				? List.of()
				: propertyMediaRepository.findByPropertyIdInAndStatusOrderByPropertyIdAscPositionAsc(
						propertyIds, PropertyMediaStatus.READY);
		return listings.map(listing -> PublicListingResponse.from(
				listing,
				media.stream()
						.filter(item -> listing.getProperty() != null
								&& item.getProperty().getId().equals(listing.getProperty().getId()))
						.toList()));
	}

	private List<PropertyMedia> publicMediaFor(Listing listing) {
		if (listing.getProperty() == null) {
			return List.of();
		}
		return propertyMediaRepository.findByOrganizationIdAndPropertyIdAndStatusOrderByPositionAsc(
				listing.getOrganization().getId(),
				listing.getProperty().getId(),
				PropertyMediaStatus.READY);
	}

	private Property resolveProperty(Long organizationId, Long propertyId) {
		if (propertyId == null) {
			return null;
		}
		return propertyRepository.findByOrganizationIdAndId(organizationId, propertyId)
				.orElseThrow(() -> new PropertyNotFoundException(propertyId, organizationId));
	}

	private void requireVersion(Listing listing, Long requestedVersion) {
		if (requestedVersion == null || listing.getVersion() != requestedVersion) {
			throw new ListingVersionConflictException(listing.getId());
		}
	}

	private String titleKey(String title) {
		if (title == null) {
			return "";
		}
		return title.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	private String normalizeSearch(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
	}

	private boolean isDuplicateTitleViolation(DataIntegrityViolationException exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof org.hibernate.exception.ConstraintViolationException constraintException) {
				String constraintName = constraintException.getConstraintName();
				if (constraintName != null && "uk_listings_org_title_key".equalsIgnoreCase(constraintName.trim())) {
					return true;
				}
			}
			String message = current.getMessage();
			if (message != null && message.toLowerCase(Locale.ROOT).contains("uk_listings_org_title_key")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
