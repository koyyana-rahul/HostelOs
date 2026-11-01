package com.rahul.HostelOs.propertymedia;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PropertyMediaRepository extends JpaRepository<PropertyMedia, Long> {

	@EntityGraph(attributePaths = {"organization", "property"})
	List<PropertyMedia> findByOrganizationIdAndPropertyIdAndStatusOrderByPositionAsc(
			Long organizationId,
			Long propertyId,
			PropertyMediaStatus status);

	List<PropertyMedia> findByPropertyIdInAndStatusOrderByPropertyIdAscPositionAsc(
			Collection<Long> propertyIds,
			PropertyMediaStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"organization", "property"})
	@Query("""
			SELECT media
			FROM PropertyMedia media
			WHERE media.organization.id = :organizationId
			  AND media.property.id = :propertyId
			  AND media.status = :status
			ORDER BY media.position ASC
			""")
	List<PropertyMedia> findByOrganizationIdAndPropertyIdAndStatusWithLock(
			@Param("organizationId") Long organizationId,
			@Param("propertyId") Long propertyId,
			@Param("status") PropertyMediaStatus status);

	@EntityGraph(attributePaths = {"organization", "property"})
	Optional<PropertyMedia> findByOrganizationIdAndPropertyIdAndId(
			Long organizationId,
			Long propertyId,
			Long id);

	long countByOrganizationIdAndPropertyIdAndStatus(
			Long organizationId,
			Long propertyId,
			PropertyMediaStatus status);

	long countByOrganizationIdAndPropertyIdAndStatusIn(
			Long organizationId,
			Long propertyId,
			Collection<PropertyMediaStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"organization", "property"})
	@Query("""
			SELECT media
			FROM PropertyMedia media
			WHERE media.organization.id = :organizationId
			  AND media.property.id = :propertyId
			  AND media.id = :id
			""")
	Optional<PropertyMedia> findByOrganizationIdAndPropertyIdAndIdWithLock(
			@Param("organizationId") Long organizationId,
			@Param("propertyId") Long propertyId,
			@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT media
			FROM PropertyMedia media
			WHERE media.id = :id
			""")
	Optional<PropertyMedia> findByIdWithLock(@Param("id") Long id);

	@Query("""
			SELECT media
			FROM PropertyMedia media
			WHERE media.status = :status
			  AND media.uploadExpiresAt IS NOT NULL
			  AND media.uploadExpiresAt <= :cutoff
			ORDER BY media.id ASC
			""")
	List<PropertyMedia> findExpiredByStatus(
			@Param("status") PropertyMediaStatus status,
			@Param("cutoff") LocalDateTime cutoff,
			Pageable pageable);

	@Modifying
	@Transactional
	@Query("""
			UPDATE PropertyMedia media
			SET media.status = :claimedStatus,
			    media.primaryImage = false
			WHERE media.id = :id
			  AND media.status = :expectedStatus
			  AND media.uploadExpiresAt IS NOT NULL
			  AND media.uploadExpiresAt <= :cutoff
			""")
	int claimExpiredForCleanup(
			@Param("id") Long id,
			@Param("expectedStatus") PropertyMediaStatus expectedStatus,
			@Param("claimedStatus") PropertyMediaStatus claimedStatus,
			@Param("cutoff") LocalDateTime cutoff);
}
