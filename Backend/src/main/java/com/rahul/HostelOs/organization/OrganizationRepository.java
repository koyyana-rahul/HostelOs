package com.rahul.HostelOs.organization;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

	Optional<Organization> findBySlug(String slug);

	boolean existsBySlug(String slug);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT o FROM Organization o WHERE o.id = :id")
	Optional<Organization> findByIdWithLock(@Param("id") Long id);
}
