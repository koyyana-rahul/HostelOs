package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.UserAccount;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {

	@EntityGraph(attributePaths = {"organization", "user"})
	Optional<OrganizationMembership> findByOrganizationIdAndUserId(Long organizationId, Long userId);

	@EntityGraph(attributePaths = {"organization", "user"})
	Optional<OrganizationMembership> findByOrganizationAndUser(Organization organization, UserAccount user);

	@EntityGraph(attributePaths = {"organization", "user"})
	Optional<OrganizationMembership> findByOrganizationIdAndUserIdAndStatus(
			Long organizationId, Long userId, MembershipStatus status);

	@EntityGraph(attributePaths = {"organization"})
	List<OrganizationMembership> findByUserId(Long userId);

	@EntityGraph(attributePaths = {"organization", "user"})
	List<OrganizationMembership> findByUserIdAndStatus(Long userId, MembershipStatus status);

	@EntityGraph(attributePaths = {"organization", "user"})
	List<OrganizationMembership> findByOrganizationId(Long organizationId);

	@EntityGraph(attributePaths = {"organization", "user"})
	List<OrganizationMembership> findByOrganizationIdAndStatus(Long organizationId, MembershipStatus status);

	boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);

	boolean existsByOrganizationIdAndUserIdAndStatus(Long organizationId, Long userId, MembershipStatus status);

	@Query("SELECT COUNT(m) FROM OrganizationMembership m " +
			"WHERE m.organization.id = :orgId " +
			"AND m.orgRole = com.rahul.HostelOs.organization.OrgRole.ORGANIZATION_ADMIN " +
			"AND m.status = com.rahul.HostelOs.organization.MembershipStatus.ACTIVE")
	long countActiveAdmins(@Param("orgId") Long orgId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"organization", "user"})
	@Query("SELECT m FROM OrganizationMembership m WHERE m.id = :id AND m.organization.id = :orgId")
	Optional<OrganizationMembership> findByIdAndOrganizationIdWithLock(
			@Param("id") Long id,
			@Param("orgId") Long orgId);
}
