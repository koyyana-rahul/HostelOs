package com.rahul.HostelOs.enquiry;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

	@EntityGraph(attributePaths = {"organization", "listing"})
	List<Enquiry> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

	@EntityGraph(attributePaths = {"organization", "listing"})
	List<Enquiry> findByOrganizationIdAndStatusOrderByCreatedAtDesc(Long organizationId, EnquiryStatus status);

	@EntityGraph(attributePaths = {"organization", "listing"})
	Optional<Enquiry> findByOrganizationIdAndId(Long organizationId, Long id);

	@EntityGraph(attributePaths = {"organization", "listing"})
	Page<Enquiry> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId, Pageable pageable);

	@EntityGraph(attributePaths = {"organization", "listing"})
	Page<Enquiry> findByCustomerUserIdAndStatusOrderByCreatedAtDesc(
			Long customerUserId,
			EnquiryStatus status,
			Pageable pageable);

	@EntityGraph(attributePaths = {"organization", "listing"})
	Optional<Enquiry> findByCustomerUserIdAndId(Long customerUserId, Long id);

	long countByOrganizationId(Long organizationId);

	long countByOrganizationIdAndStatus(Long organizationId, EnquiryStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"organization", "listing"})
	@Query("SELECT e FROM Enquiry e WHERE e.id = :id AND e.organization.id = :organizationId")
	Optional<Enquiry> findByIdAndOrganizationIdWithLock(
			@Param("id") Long id,
			@Param("organizationId") Long organizationId);
}
