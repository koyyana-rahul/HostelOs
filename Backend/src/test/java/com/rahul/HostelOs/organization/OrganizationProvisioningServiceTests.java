package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.Role;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.RoleRepository;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.organization.dto.CreateOrganizationRequest;
import com.rahul.HostelOs.organization.dto.OrganizationResponse;
import com.rahul.HostelOs.organization.exception.DuplicateOrganizationSlugException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
class OrganizationProvisioningServiceTests {

	@Autowired
	private OrganizationProvisioningService provisioningService;

	@MockitoSpyBean
	private OrganizationRepository organizationRepository;

	@MockitoSpyBean
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private RoleRepository roleRepository;

	private UserAccount activeCreator;
	private UserAccount disabledCreator;

	@BeforeEach
	void setUp() {
		reset(organizationRepository);
		reset(membershipRepository);

		Role staffRole = roleRepository.findByName(RoleName.STAFF).orElseThrow();
		activeCreator = userAccountRepository.save(new UserAccount(
				"active-creator-" + System.nanoTime() + "@example.com",
				"Active Creator",
				"$2a$12$01234567890123456789012345678901234567890123456789012",
				AccountStatus.ACTIVE,
				Set.of(staffRole)));

		disabledCreator = userAccountRepository.save(new UserAccount(
				"disabled-creator-" + System.nanoTime() + "@example.com",
				"Disabled Creator",
				"$2a$12$01234567890123456789012345678901234567890123456789012",
				AccountStatus.DISABLED,
				Set.of(staffRole)));
	}

	@Test
	@DisplayName("Successfully provisions organization and creator ORGANIZATION_ADMIN membership atomically")
	void provisionOrganization_Success() {
		String slug = "sunrise-hostel-" + System.nanoTime();
		CreateOrganizationRequest request = new CreateOrganizationRequest("Sunrise Hostel", slug);

		OrganizationResponse response = provisioningService.provisionOrganization(request, activeCreator.getId());

		assertThat(response).isNotNull();
		assertThat(response.id()).isNotNull();
		assertThat(response.name()).isEqualTo("Sunrise Hostel");
		assertThat(response.slug()).isEqualTo(slug);
		assertThat(response.status()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(response.createdAt()).isNotNull();

		// Verify organization row exists
		Organization organization = organizationRepository.findById(response.id()).orElseThrow();
		assertThat(organization.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
		assertThat(organization.getSlug()).isEqualTo(slug);

		// Verify creator membership exists with ORGANIZATION_ADMIN and ACTIVE
		List<OrganizationMembership> memberships = membershipRepository.findByOrganizationId(response.id());
		assertThat(memberships).hasSize(1);
		OrganizationMembership creatorMembership = memberships.get(0);
		assertThat(creatorMembership.getUser().getId()).isEqualTo(activeCreator.getId());
		assertThat(creatorMembership.getOrgRole()).isEqualTo(OrgRole.ORGANIZATION_ADMIN);
		assertThat(creatorMembership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
	}

	@Test
	@DisplayName("Rejects null request DTO")
	void provisionOrganization_NullRequest_ThrowsException() {
		assertThatThrownBy(() -> provisioningService.provisionOrganization(null, activeCreator.getId()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("CreateOrganizationRequest must not be null");
	}

	@Test
	@DisplayName("Rejects non-positive or null creator user ID")
	void provisionOrganization_InvalidCreatorId_ThrowsException() {
		CreateOrganizationRequest request = new CreateOrganizationRequest("Sunrise Hostel", "sunrise-hostel");

		assertThatThrownBy(() -> provisioningService.provisionOrganization(request, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("positive non-null value");

		assertThatThrownBy(() -> provisioningService.provisionOrganization(request, 0L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("positive non-null value");

		assertThatThrownBy(() -> provisioningService.provisionOrganization(request, -5L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("positive non-null value");
	}

	@Test
	@DisplayName("Rejects blank or oversized organization name")
	void provisionOrganization_InvalidName_ThrowsException() {
		String slug = "valid-slug-" + System.nanoTime();

		assertThatThrownBy(() -> provisioningService.provisionOrganization(new CreateOrganizationRequest("", slug), activeCreator.getId()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must not be blank");

		assertThatThrownBy(() -> provisioningService.provisionOrganization(new CreateOrganizationRequest("   ", slug), activeCreator.getId()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must not be blank");

		String oversizedName = "A".repeat(121);
		assertThatThrownBy(() -> provisioningService.provisionOrganization(new CreateOrganizationRequest(oversizedName, slug), activeCreator.getId()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must not exceed 120 characters");
	}

	@Test
	@DisplayName("Rejects malformed, too short, or too long slug")
	void provisionOrganization_InvalidSlug_ThrowsException() {
		assertThatThrownBy(() -> provisioningService.provisionOrganization(new CreateOrganizationRequest("Test Org", "ab"), activeCreator.getId()))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> provisioningService.provisionOrganization(new CreateOrganizationRequest("Test Org", "-invalid-start"), activeCreator.getId()))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> provisioningService.provisionOrganization(new CreateOrganizationRequest("Test Org", "invalid--double-hyphen"), activeCreator.getId()))
				.isInstanceOf(IllegalArgumentException.class);

		String oversizedSlug = "a".repeat(61);
		assertThatThrownBy(() -> provisioningService.provisionOrganization(new CreateOrganizationRequest("Test Org", oversizedSlug), activeCreator.getId()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("Rejects non-existent creator ID")
	void provisionOrganization_NonExistentCreator_ThrowsException() {
		String slug = "non-existent-creator-" + System.nanoTime();
		CreateOrganizationRequest request = new CreateOrganizationRequest("Valid Org", slug);

		assertThatThrownBy(() -> provisioningService.provisionOrganization(request, 999999L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Creator user account not found");

		assertThat(organizationRepository.findBySlug(slug)).isEmpty();
	}

	@Test
	@DisplayName("Rejects disabled creator account")
	void provisionOrganization_DisabledCreator_ThrowsException() {
		String slug = "disabled-creator-" + System.nanoTime();
		CreateOrganizationRequest request = new CreateOrganizationRequest("Valid Org", slug);

		assertThatThrownBy(() -> provisioningService.provisionOrganization(request, disabledCreator.getId()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Creator account is not active");

		assertThat(organizationRepository.findBySlug(slug)).isEmpty();
	}

	@Test
	@DisplayName("Pre-check rejects duplicate slug with DuplicateOrganizationSlugException")
	void provisionOrganization_DuplicateSlugPreCheck_ThrowsException() {
		String slug = "duplicate-slug-" + System.nanoTime();
		CreateOrganizationRequest first = new CreateOrganizationRequest("First Org", slug);
		provisioningService.provisionOrganization(first, activeCreator.getId());

		CreateOrganizationRequest second = new CreateOrganizationRequest("Second Org", slug);
		assertThatThrownBy(() -> provisioningService.provisionOrganization(second, activeCreator.getId()))
				.isInstanceOf(DuplicateOrganizationSlugException.class)
				.hasMessageContaining("Organization slug already exists: " + slug);
	}

	@Test
	@DisplayName("Unrelated persistence failure during save propagates without conversion")
	void provisionOrganization_UnrelatedPersistenceFailure_Propagates() {
		String slug = "unrelated-fail-" + System.nanoTime();
		CreateOrganizationRequest request = new CreateOrganizationRequest("Fail Org", slug);

		doThrow(new DataAccessResourceFailureException("Database connection timeout"))
				.when(membershipRepository).save(any(OrganizationMembership.class));

		assertThatThrownBy(() -> provisioningService.provisionOrganization(request, activeCreator.getId()))
				.isInstanceOf(DataAccessResourceFailureException.class)
				.hasMessageContaining("Database connection timeout");
	}

	@Test
	@DisplayName("Atomic rollback: Membership failure rolls back organization insertion")
	void provisionOrganization_MembershipFailure_RollsBackOrganization() {
		String slug = "rollback-test-" + System.nanoTime();
		CreateOrganizationRequest request = new CreateOrganizationRequest("Rollback Org", slug);

		// Force membership saving to fail inside the transaction
		doThrow(new DataAccessResourceFailureException("Simulated membership constraint or DB failure"))
				.when(membershipRepository).save(any(OrganizationMembership.class));

		assertThatThrownBy(() -> provisioningService.provisionOrganization(request, activeCreator.getId()))
				.isInstanceOf(DataAccessResourceFailureException.class)
				.hasMessageContaining("Simulated membership constraint or DB failure");

		// Crucial assertion: the organization insert must have rolled back!
		Optional<Organization> orphanedOrg = organizationRepository.findBySlug(slug);
		assertThat(orphanedOrg).as("Organization must be rolled back if membership creation fails").isEmpty();
		assertThat(organizationRepository.existsBySlug(slug)).isFalse();
	}

	@Test
	@DisplayName("Simulated concurrent race: unique constraint uk_organizations_slug violation translates to DuplicateOrganizationSlugException")
	void provisionOrganization_ConcurrentSlugRace_TranslatesToDuplicateException() {
		String slug = "race-slug-" + System.nanoTime();
		// First organization already exists in DB
		organizationRepository.save(new Organization("Existing Org", slug, OrganizationStatus.ACTIVE));

		// Simulate pre-check race where existsBySlug returns false
		doReturn(false).when(organizationRepository).existsBySlug(slug);

		CreateOrganizationRequest request = new CreateOrganizationRequest("Second Org", slug);
		assertThatThrownBy(() -> provisioningService.provisionOrganization(request, activeCreator.getId()))
				.isInstanceOf(DuplicateOrganizationSlugException.class)
				.hasMessageContaining("Organization slug already exists: " + slug);
	}

	@Test
	@DisplayName("Slug check/format constraint violation is not misclassified as a duplicate slug")
	void provisionOrganization_CheckConstraintViolation_PropagatesUnchanged() {
		String slug = "check-violation-" + System.nanoTime();
		CreateOrganizationRequest request = new CreateOrganizationRequest("Check Org", slug);

		// Throw a DataIntegrityViolationException referencing chk_organizations_slug (not uk_organizations_slug)
		doThrow(new DataIntegrityViolationException("Check constraint violation: chk_organizations_slug"))
				.when(membershipRepository).flush();

		assertThatThrownBy(() -> provisioningService.provisionOrganization(request, activeCreator.getId()))
				.isInstanceOf(DataIntegrityViolationException.class)
				.isNotInstanceOf(DuplicateOrganizationSlugException.class)
				.hasMessageContaining("chk_organizations_slug");
	}

	@Test
	@DisplayName("Unrelated unique constraint violation is not misclassified as a duplicate slug")
	void provisionOrganization_UnrelatedConstraintViolation_PropagatesUnchanged() {
		String slug = "unrelated-constraint-" + System.nanoTime();
		CreateOrganizationRequest request = new CreateOrganizationRequest("Unrelated Org", slug);

		doThrow(new DataIntegrityViolationException("Unique index violation: uk_org_memberships_org_user"))
				.when(membershipRepository).flush();

		assertThatThrownBy(() -> provisioningService.provisionOrganization(request, activeCreator.getId()))
				.isInstanceOf(DataIntegrityViolationException.class)
				.isNotInstanceOf(DuplicateOrganizationSlugException.class)
				.hasMessageContaining("uk_org_memberships_org_user");
	}
}
