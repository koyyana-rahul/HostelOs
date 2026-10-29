package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

	@EntityGraph(attributePaths = {"organization"})
	List<OrganizationMembership> findByUserIdAndStatus(Long userId, MembershipStatus status);

	@EntityGraph(attributePaths = {"user"})
	List<OrganizationMembership> findByOrganizationId(Long organizationId);

	@EntityGraph(attributePaths = {"user"})
	List<OrganizationMembership> findByOrganizationIdAndStatus(Long organizationId, MembershipStatus status);

	boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);

	boolean existsByOrganizationIdAndUserIdAndStatus(Long organizationId, Long userId, MembershipStatus status);
}
