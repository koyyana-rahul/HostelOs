package com.rahul.HostelOs.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, Long> {

	@EntityGraph(attributePaths = {"organization"})
	Optional<Property> findByOrganizationIdAndId(Long organizationId, Long id);

	@EntityGraph(attributePaths = {"organization"})
	Optional<Property> findByOrganizationIdAndCode(Long organizationId, String code);

	@EntityGraph(attributePaths = {"organization"})
	List<Property> findByOrganizationId(Long organizationId);

	@EntityGraph(attributePaths = {"organization"})
	List<Property> findByOrganizationIdAndStatus(Long organizationId, PropertyStatus status);

	boolean existsByOrganizationIdAndCode(Long organizationId, String code);

	boolean existsByOrganizationIdAndId(Long organizationId, Long id);

	long countByOrganizationId(Long organizationId);

	long countByOrganizationIdAndStatus(Long organizationId, PropertyStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"organization"})
	@Query("SELECT p FROM Property p WHERE p.id = :id AND p.organization.id = :organizationId")
	Optional<Property> findByIdAndOrganizationIdWithLock(
			@Param("id") Long id,
			@Param("organizationId") Long organizationId);
}
