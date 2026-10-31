package com.rahul.HostelOs.building;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BuildingRepository extends JpaRepository<Building, Long> {

	@EntityGraph(attributePaths = {"property"})
	Optional<Building> findByPropertyIdAndId(Long propertyId, Long id);

	@EntityGraph(attributePaths = {"property"})
	List<Building> findByPropertyId(Long propertyId);

	@EntityGraph(attributePaths = {"property"})
	List<Building> findByPropertyIdAndStatus(Long propertyId, BuildingStatus status);

	boolean existsByPropertyIdAndCode(Long propertyId, String code);

	boolean existsByPropertyIdAndId(Long propertyId, Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"property"})
	@Query("SELECT b FROM Building b WHERE b.id = :id AND b.property.id = :propertyId")
	Optional<Building> findByIdAndPropertyIdWithLock(
			@Param("id") Long id,
			@Param("propertyId") Long propertyId);
}
