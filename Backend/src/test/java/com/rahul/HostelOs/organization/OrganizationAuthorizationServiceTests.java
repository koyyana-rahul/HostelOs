package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.identity.Role;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.UserAccount;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationAuthorizationServiceTests {

	@Mock
	private OrganizationRepository organizationRepository;

	@Mock
	private OrganizationMembershipRepository membershipRepository;

	private OrganizationAuthorizationService service;

	private Organization activeOrg;
	private Organization suspendedOrg;
	private Organization deactivatedOrg;
	private UserAccount activeUser;

	@BeforeEach
	void setUp() {
		service = new OrganizationAuthorizationService(organizationRepository, membershipRepository);

		activeOrg = new Organization("Alpha Hostel", "alpha-hostel", OrganizationStatus.ACTIVE);
		ReflectionTestUtils.setField(activeOrg, "id", 10L);

		suspendedOrg = new Organization("Beta Hostel", "beta-hostel", OrganizationStatus.SUSPENDED);
		ReflectionTestUtils.setField(suspendedOrg, "id", 20L);

		deactivatedOrg = new Organization("Gamma Hostel", "gamma-hostel", OrganizationStatus.DEACTIVATED);
		ReflectionTestUtils.setField(deactivatedOrg, "id", 30L);

		Role staffRole = createRole(RoleName.STAFF);

		activeUser = new UserAccount(
				"user@example.com",
				"Test User",
				"$2a$12$01234567890123456789012345678901234567890123456789012",
				AccountStatus.ACTIVE,
				Set.of(staffRole));
		ReflectionTestUtils.setField(activeUser, "id", 100L);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private Role createRole(RoleName name) {
		try {
			java.lang.reflect.Constructor<Role> constructor = Role.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			Role r = constructor.newInstance();
			ReflectionTestUtils.setField(r, "name", name);
			ReflectionTestUtils.setField(r, "description", name.name());
			return r;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to instantiate Role via reflection", e);
		}
	}

	private void setAuthenticatedUser(Long userId, RoleName... globalRoles) {
		Set<Role> roles = new HashSet<>();
		for (RoleName rn : globalRoles) {
			roles.add(createRole(rn));
		}
		UserAccount account = new UserAccount(
				"test-" + userId + "@example.com",
				"Test User " + userId,
				"$2a$12$01234567890123456789012345678901234567890123456789012",
				AccountStatus.ACTIVE,
				roles);
		ReflectionTestUtils.setField(account, "id", userId);

		HostelOsPrincipal principal = HostelOsPrincipal.from(account);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
		SecurityContextHolder.setContext(context);
	}

	@Test
	@DisplayName("Null organization ID returns false without querying repositories")
	void canAccessOrg_NullOrgId_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);

		boolean authorized = service.canAccessOrg(null, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
		verifyNoInteractions(organizationRepository, membershipRepository);
	}

	@Test
	@DisplayName("Zero or negative organization ID returns false without querying repositories")
	void canAccessOrg_NonPositiveOrgId_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);

		assertThat(service.canAccessOrg(0L, OrgRole.ORG_STAFF)).isFalse();
		assertThat(service.canAccessOrg(-1L, OrgRole.ORG_STAFF)).isFalse();
		assertThat(service.canAccessOrg(-999L, OrgRole.ORG_ADMIN)).isFalse();
		verifyNoInteractions(organizationRepository, membershipRepository);
	}

	@Test
	@DisplayName("Null required role returns false")
	void canAccessOrg_NullRequiredRole_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);

		boolean authorized = service.canAccessOrg(10L, (OrgRole) null);

		assertThat(authorized).isFalse();
		verifyNoInteractions(organizationRepository, membershipRepository);
	}

	@Test
	@DisplayName("Missing SecurityContext authentication returns false")
	void canAccessOrg_NoAuthentication_ReturnsFalse() {
		SecurityContextHolder.clearContext();

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
		verifyNoInteractions(organizationRepository, membershipRepository);
	}

	@Test
	@DisplayName("Unauthenticated authentication token returns false")
	void canAccessOrg_UnauthenticatedToken_ReturnsFalse() {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		Authentication auth = UsernamePasswordAuthenticationToken.unauthenticated("user@example.com", "pass");
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
		verifyNoInteractions(organizationRepository, membershipRepository);
	}

	@Test
	@DisplayName("Anonymous authentication token returns false")
	void canAccessOrg_AnonymousToken_ReturnsFalse() {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		Authentication auth = new AnonymousAuthenticationToken(
				"key",
				"anonymousUser",
				List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
		verifyNoInteractions(organizationRepository, membershipRepository);
	}

	@Test
	@DisplayName("Non-HostelOsPrincipal principal returns false")
	void canAccessOrg_NonHostelOsPrincipal_ReturnsFalse() {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		Authentication auth = new UsernamePasswordAuthenticationToken("just-a-string-principal", null, List.of());
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
		verifyNoInteractions(organizationRepository, membershipRepository);
	}

	@Test
	@DisplayName("Principal with null user ID returns false")
	void canAccessOrg_NullUserIdInPrincipal_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		HostelOsPrincipal currentPrincipal = (HostelOsPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		ReflectionTestUtils.setField(currentPrincipal, "userId", null);

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
		verifyNoInteractions(organizationRepository, membershipRepository);
	}

	@Test
	@DisplayName("Non-existent organization returns false")
	void canAccessOrg_OrganizationNotFound_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

		boolean authorized = service.canAccessOrg(999L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
		verify(organizationRepository).findById(999L);
		verifyNoInteractions(membershipRepository);
	}

	@Test
	@DisplayName("Suspended organization returns false")
	void canAccessOrg_OrganizationSuspended_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(20L)).thenReturn(Optional.of(suspendedOrg));

		boolean authorized = service.canAccessOrg(20L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
		verify(organizationRepository).findById(20L);
		verifyNoInteractions(membershipRepository);
	}

	@Test
	@DisplayName("Deactivated organization returns false")
	void canAccessOrg_OrganizationDeactivated_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(30L)).thenReturn(Optional.of(deactivatedOrg));

		boolean authorized = service.canAccessOrg(30L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
		verify(organizationRepository).findById(30L);
		verifyNoInteractions(membershipRepository);
	}

	@Test
	@DisplayName("Caller without membership in organization returns false")
	void canAccessOrg_MembershipNotFound_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 100L, MembershipStatus.ACTIVE))
				.thenReturn(Optional.empty());

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
		verify(organizationRepository).findById(10L);
		verify(membershipRepository).findByOrganizationIdAndUserIdAndStatus(10L, 100L, MembershipStatus.ACTIVE);
	}

	@Test
	@DisplayName("Suspended membership (not returning active status) returns false")
	void canAccessOrg_SuspendedMembership_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 100L, MembershipStatus.ACTIVE))
				.thenReturn(Optional.empty());

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
	}

	@Test
	@DisplayName("Revoked membership (not returning active status) returns false")
	void canAccessOrg_RevokedMembership_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 100L, MembershipStatus.ACTIVE))
				.thenReturn(Optional.empty());

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_STAFF);

		assertThat(authorized).isFalse();
	}

	@Test
	@DisplayName("Global PLATFORM_ADMIN without membership in organization returns false (no automatic bypass)")
	void canAccessOrg_PlatformAdminWithoutMembership_ReturnsFalse() {
		setAuthenticatedUser(999L, RoleName.PLATFORM_ADMIN);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 999L, MembershipStatus.ACTIVE))
				.thenReturn(Optional.empty());

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_ADMIN);

		assertThat(authorized).isFalse();
		verify(organizationRepository).findById(10L);
		verify(membershipRepository).findByOrganizationIdAndUserIdAndStatus(10L, 999L, MembershipStatus.ACTIVE);
	}

	@Test
	@DisplayName("Global ORGANIZATION_ADMIN without membership in organization returns false (no automatic bypass)")
	void canAccessOrg_GlobalOrgAdminWithoutMembership_ReturnsFalse() {
		setAuthenticatedUser(888L, RoleName.ORGANIZATION_ADMIN);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 888L, MembershipStatus.ACTIVE))
				.thenReturn(Optional.empty());

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_ADMIN);

		assertThat(authorized).isFalse();
		verify(organizationRepository).findById(10L);
		verify(membershipRepository).findByOrganizationIdAndUserIdAndStatus(10L, 888L, MembershipStatus.ACTIVE);
	}

	@Test
	@DisplayName("Member with ORG_ADMIN satisfies ORG_ADMIN requirement")
	void canAccessOrg_AdminMembership_RequiresAdmin_ReturnsTrue() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		OrganizationMembership membership = new OrganizationMembership(
				activeOrg, activeUser, OrgRole.ORG_ADMIN, MembershipStatus.ACTIVE);
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 100L, MembershipStatus.ACTIVE))
				.thenReturn(Optional.of(membership));

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_ADMIN);

		assertThat(authorized).isTrue();
	}

	@Test
	@DisplayName("Member with ORG_ADMIN satisfies ORG_STAFF requirement")
	void canAccessOrg_AdminMembership_RequiresStaff_ReturnsTrue() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		OrganizationMembership membership = new OrganizationMembership(
				activeOrg, activeUser, OrgRole.ORG_ADMIN, MembershipStatus.ACTIVE);
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 100L, MembershipStatus.ACTIVE))
				.thenReturn(Optional.of(membership));

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_STAFF);

		assertThat(authorized).isTrue();
	}

	@Test
	@DisplayName("Member with ORG_STAFF satisfies ORG_STAFF requirement")
	void canAccessOrg_StaffMembership_RequiresStaff_ReturnsTrue() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		OrganizationMembership membership = new OrganizationMembership(
				activeOrg, activeUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE);
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 100L, MembershipStatus.ACTIVE))
				.thenReturn(Optional.of(membership));

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_STAFF);

		assertThat(authorized).isTrue();
	}

	@Test
	@DisplayName("Member with ORG_STAFF fails ORG_ADMIN requirement")
	void canAccessOrg_StaffMembership_RequiresAdmin_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		OrganizationMembership membership = new OrganizationMembership(
				activeOrg, activeUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE);
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 100L, MembershipStatus.ACTIVE))
				.thenReturn(Optional.of(membership));

		boolean authorized = service.canAccessOrg(10L, OrgRole.ORG_ADMIN);

		assertThat(authorized).isFalse();
	}

	@Test
	@DisplayName("String overload correctly resolves valid OrgRole names")
	void canAccessOrg_StringOverload_ValidRole_BehavesIdentically() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		OrganizationMembership membership = new OrganizationMembership(
				activeOrg, activeUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE);
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 100L, MembershipStatus.ACTIVE))
				.thenReturn(Optional.of(membership));

		assertThat(service.canAccessOrg(10L, "ORG_STAFF")).isTrue();
		assertThat(service.canAccessOrg(10L, "ORG_ADMIN")).isFalse();
	}

	@Test
	@DisplayName("String overload with invalid, null, or blank role name returns false")
	void canAccessOrg_StringOverload_InvalidRole_ReturnsFalse() {
		setAuthenticatedUser(100L, RoleName.STAFF);

		assertThat(service.canAccessOrg(10L, "NON_EXISTENT_ROLE")).isFalse();
		assertThat(service.canAccessOrg(10L, (String) null)).isFalse();
		assertThat(service.canAccessOrg(10L, "")).isFalse();
		assertThat(service.canAccessOrg(10L, "   ")).isFalse();
		verifyNoInteractions(organizationRepository, membershipRepository);
	}

	@Test
	@DisplayName("Unexpected database exception from organizationRepository propagates without conversion")
	void canAccessOrg_OrganizationRepoThrowsException_Propagates() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(10L))
				.thenThrow(new DataAccessResourceFailureException("DB connection failure"));

		assertThatThrownBy(() -> service.canAccessOrg(10L, OrgRole.ORG_STAFF))
				.isInstanceOf(DataAccessResourceFailureException.class)
				.hasMessageContaining("DB connection failure");
	}

	@Test
	@DisplayName("Unexpected database exception from membershipRepository propagates without conversion")
	void canAccessOrg_MembershipRepoThrowsException_Propagates() {
		setAuthenticatedUser(100L, RoleName.STAFF);
		when(organizationRepository.findById(10L)).thenReturn(Optional.of(activeOrg));
		when(membershipRepository.findByOrganizationIdAndUserIdAndStatus(10L, 100L, MembershipStatus.ACTIVE))
				.thenThrow(new DataAccessResourceFailureException("DB query timeout"));

		assertThatThrownBy(() -> service.canAccessOrg(10L, OrgRole.ORG_STAFF))
				.isInstanceOf(DataAccessResourceFailureException.class)
				.hasMessageContaining("DB query timeout");
	}
}
