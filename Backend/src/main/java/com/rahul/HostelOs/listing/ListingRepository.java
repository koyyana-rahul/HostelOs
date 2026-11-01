package com.rahul.HostelOs.listing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ListingRepository extends JpaRepository<Listing, Long> {

	@EntityGraph(attributePaths = {"organization", "property"})
	List<Listing> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

	@EntityGraph(attributePaths = {"organization", "property"})
	List<Listing> findByOrganizationIdAndStatusOrderByCreatedAtDesc(Long organizationId, ListingStatus status);

	@EntityGraph(attributePaths = {"organization", "property"})
	Optional<Listing> findByOrganizationIdAndId(Long organizationId, Long id);

	@EntityGraph(attributePaths = {"organization", "property"})
	Optional<Listing> findByIdAndStatus(Long id, ListingStatus status);

	boolean existsByOrganizationIdAndTitleKey(Long organizationId, String titleKey);

	boolean existsByOrganizationIdAndTitleKeyAndIdNot(Long organizationId, String titleKey, Long id);

	long countByOrganizationId(Long organizationId);

	long countByOrganizationIdAndStatus(Long organizationId, ListingStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"organization", "property"})
	@Query("SELECT l FROM Listing l WHERE l.id = :id AND l.organization.id = :organizationId")
	Optional<Listing> findByIdAndOrganizationIdWithLock(
			@Param("id") Long id,
			@Param("organizationId") Long organizationId);

	@EntityGraph(attributePaths = {"organization", "property"})
	@Query("""
			SELECT l FROM Listing l
			WHERE l.status = com.rahul.HostelOs.listing.ListingStatus.PUBLISHED
			AND (:query IS NULL OR LOWER(l.title) LIKE :query OR LOWER(l.description) LIKE :query OR LOWER(l.city) LIKE :query)
			AND (:city IS NULL OR LOWER(l.city) = :city)
			AND (:type IS NULL OR l.accommodationType = :type)
			AND (:minPrice IS NULL OR l.basePrice >= :minPrice)
			AND (:maxPrice IS NULL OR l.basePrice <= :maxPrice)
			""")
	Page<Listing> searchPublished(
			@Param("query") String query,
			@Param("city") String city,
			@Param("type") AccommodationType type,
			@Param("minPrice") BigDecimal minPrice,
			@Param("maxPrice") BigDecimal maxPrice,
			Pageable pageable);
}
