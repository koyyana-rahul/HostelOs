package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.Role;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.RoleRepository;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationRepositoryTests {

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private RoleRepository roleRepository;

	private UserAccount testUser;
	private Role staffRole;

	@BeforeEach
	void setUp() {
		staffRole = roleRepository.findByName(RoleName.STAFF)
				.orElseThrow(() -> new IllegalStateException("STAFF role missing"));
		testUser = userAccountRepository.save(new UserAccount(
				"user-" + System.nanoTime() + "@example.com",
				"Test User",
				"$2a$12$01234567890123456789012345678901234567890123456789012",
				AccountStatus.ACTIVE,
				Set.of(staffRole)));
	}

	@Test
	void organizationPersistsAndCanBeRetrieved() {
		Organization org = new Organization("Sunrise Hostel", "sunrise-hostel", OrganizationStatus.ACTIVE);
		Organization saved = organizationRepository.save(org);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getVersion()).isZero();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();

		Optional<Organization> bySlug = organizationRepository.findBySlug("sunrise-hostel");
		assertThat(bySlug).isPresent();
		assertThat(bySlug.get().getName()).isEqualTo("Sunrise Hostel");
		assertThat(bySlug.get().getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
	}

	@Test
	void slugNormalizationIsConsistentAndDeterministic() {
		Organization org = new Organization("Sunrise Hostel", "  ＳＵＮＲＩＳＥ-ＨＯＳＴＥＬ  ", OrganizationStatus.ACTIVE);
		Organization saved = organizationRepository.save(org);

		assertThat(saved.getSlug()).isEqualTo("sunrise-hostel");
		assertThat(organizationRepository.findBySlug("sunrise-hostel")).isPresent();
	}

	@Test
	void invalidSlugFormatIsRejected() {
		assertThatThrownBy(() -> new Organization("Invalid Org", "invalid_slug_with_underscore", OrganizationStatus.ACTIVE))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> new Organization("Invalid Org", "ab", OrganizationStatus.ACTIVE))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void duplicateNormalizedSlugIsRejected() {
		Organization org1 = new Organization("Org One", "duplicate-slug", OrganizationStatus.ACTIVE);
		organizationRepository.saveAndFlush(org1);

		Organization org2 = new Organization("Org Two", "DUPLICATE-SLUG", OrganizationStatus.ACTIVE);
		assertThatThrownBy(() -> organizationRepository.saveAndFlush(org2))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void organizationMembershipCorrectlyReferencesUserAndOrganization() {
		Organization org = organizationRepository.save(new Organization("Alpha Hostel", "alpha-hostel", OrganizationStatus.ACTIVE));

		OrganizationMembership membership = new OrganizationMembership(
				org, testUser, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.ACTIVE);
		OrganizationMembership saved = membershipRepository.save(membership);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getOrganization().getId()).isEqualTo(org.getId());
		assertThat(saved.getUser().getId()).isEqualTo(testUser.getId());
		assertThat(saved.getOrgRole()).isEqualTo(OrgRole.ORGANIZATION_ADMIN);
		assertThat(saved.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
	}

	@Test
	void duplicateOrganizationUserMembershipIsRejected() {
		Organization org = organizationRepository.save(new Organization("Beta Hostel", "beta-hostel", OrganizationStatus.ACTIVE));

		OrganizationMembership m1 = new OrganizationMembership(org, testUser, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.ACTIVE);
		membershipRepository.saveAndFlush(m1);

		OrganizationMembership m2 = new OrganizationMembership(org, testUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE);
		assertThatThrownBy(() -> membershipRepository.saveAndFlush(m2))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void orgRolesAndStatusesMapCorrectlyToPersistence() {
		Organization org = organizationRepository.save(new Organization("Gamma Hostel", "gamma-hostel", OrganizationStatus.ACTIVE));

		UserAccount user2 = userAccountRepository.save(new UserAccount(
				"user2-" + System.nanoTime() + "@example.com",
				"Test User 2",
				"$2a$12$01234567890123456789012345678901234567890123456789012",
				AccountStatus.ACTIVE,
				Set.of(staffRole)));

		OrganizationMembership adminMember = membershipRepository.saveAndFlush(
				new OrganizationMembership(org, testUser, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.ACTIVE));
		OrganizationMembership staffMember = membershipRepository.saveAndFlush(
				new OrganizationMembership(org, user2, OrgRole.ORG_STAFF, MembershipStatus.SUSPENDED));

		assertThat(adminMember.getOrgRole()).isEqualTo(OrgRole.ORGANIZATION_ADMIN);
		assertThat(adminMember.getStatus()).isEqualTo(MembershipStatus.ACTIVE);

		assertThat(staffMember.getOrgRole()).isEqualTo(OrgRole.ORG_STAFF);
		assertThat(staffMember.getStatus()).isEqualTo(MembershipStatus.SUSPENDED);

		staffMember.revoke();
		OrganizationMembership revokedMember = membershipRepository.saveAndFlush(staffMember);
		assertThat(revokedMember.getStatus()).isEqualTo(MembershipStatus.REVOKED);
	}

	@Test
	void suspendedOrRevokedMembershipCanBeReactivatedInPlaceWithoutDuplicates() {
		Organization org = organizationRepository.save(new Organization("Delta Hostel", "delta-hostel", OrganizationStatus.ACTIVE));

		OrganizationMembership membership = membershipRepository.saveAndFlush(
				new OrganizationMembership(org, testUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));
		Long membershipId = membership.getId();

		// Revoke membership
		membership.revoke();
		membershipRepository.saveAndFlush(membership);

		OrganizationMembership revoked = membershipRepository.findById(membershipId).orElseThrow();
		assertThat(revoked.getStatus()).isEqualTo(MembershipStatus.REVOKED);

		// Reactivate membership in place with role elevation to ORGANIZATION_ADMIN
		revoked.reactivate(OrgRole.ORGANIZATION_ADMIN);
		OrganizationMembership reactivated = membershipRepository.saveAndFlush(revoked);

		assertThat(reactivated.getId()).isEqualTo(membershipId);
		assertThat(reactivated.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(reactivated.getOrgRole()).isEqualTo(OrgRole.ORGANIZATION_ADMIN);

		// Assert only one record exists for (org, user)
		List<OrganizationMembership> userMemberships = membershipRepository.findByOrganizationId(org.getId());
		assertThat(userMemberships).hasSize(1);
	}

	@Test
	void repositoryLookupsReturnExpectedEntities() {
		Organization org = organizationRepository.save(new Organization("Epsilon Hostel", "epsilon-hostel", OrganizationStatus.ACTIVE));

		OrganizationMembership membership = membershipRepository.saveAndFlush(
				new OrganizationMembership(org, testUser, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.ACTIVE));

		assertThat(membershipRepository.findByOrganizationIdAndUserId(org.getId(), testUser.getId())).isPresent();
		assertThat(membershipRepository.findByOrganizationIdAndUserIdAndStatus(org.getId(), testUser.getId(), MembershipStatus.ACTIVE)).isPresent();
		assertThat(membershipRepository.findByOrganizationIdAndUserIdAndStatus(org.getId(), testUser.getId(), MembershipStatus.REVOKED)).isEmpty();

		assertThat(membershipRepository.findByUserId(testUser.getId())).hasSize(1);
		assertThat(membershipRepository.findByOrganizationId(org.getId())).hasSize(1);

		assertThat(membershipRepository.existsByOrganizationIdAndUserId(org.getId(), testUser.getId())).isTrue();
		assertThat(membershipRepository.existsByOrganizationIdAndUserId(org.getId(), 999999L)).isFalse();
	}
}
