package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.identity.Role;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.RoleRepository;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.organization.dto.AddMemberRequest;
import com.rahul.HostelOs.organization.dto.ChangeMemberRoleRequest;
import com.rahul.HostelOs.organization.dto.MembershipResponse;
import com.rahul.HostelOs.organization.dto.ReactivateMemberRequest;
import com.rahul.HostelOs.organization.exception.InactiveUserAccountException;
import com.rahul.HostelOs.organization.exception.InvalidMembershipTransitionException;
import com.rahul.HostelOs.organization.exception.LastAdminProtectionException;
import com.rahul.HostelOs.organization.exception.MembershipAlreadyExistsException;
import com.rahul.HostelOs.organization.exception.MembershipNotFoundException;
import com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException;
import com.rahul.HostelOs.organization.exception.OrganizationNotFoundException;
import com.rahul.HostelOs.organization.exception.UserNotFoundException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
class MembershipManagementServiceTests {

	@Autowired
	private MembershipManagementService membershipService;

	@MockitoSpyBean
	private OrganizationRepository organizationRepository;

	@MockitoSpyBean
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private RoleRepository roleRepository;

	private Role staffRole;
	private Organization testOrg;
	private UserAccount adminUser;
	private OrganizationMembership adminMembership;

	@BeforeEach
	void setUp() {
		reset(organizationRepository);
		reset(membershipRepository);

		staffRole = roleRepository.findByName(RoleName.STAFF).orElseThrow();

		testOrg = organizationRepository.save(new Organization(
				"Test Org " + System.nanoTime(),
				"test-org-" + System.nanoTime(),
				OrganizationStatus.ACTIVE));

		adminUser = createUser("admin-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		adminMembership = membershipRepository.save(new OrganizationMembership(
				testOrg, adminUser, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.ACTIVE));

		authenticateAs(adminUser);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private void authenticateAs(UserAccount user) {
		HostelOsPrincipal principal = HostelOsPrincipal.from(user);
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
				principal, null, principal.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private UserAccount createUser(String email, AccountStatus status) {
		return userAccountRepository.save(new UserAccount(
				email,
				"Display " + email,
				"$2a$12$01234567890123456789012345678901234567890123456789012",
				status,
				Set.of(staffRole)));
	}

	// ==========================================
	// Group A: addMember
	// ==========================================

	@Test
	@DisplayName("A1: Add valid active user as ORG_STAFF")
	void addMember_Success_Staff() {
		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		MembershipResponse response = membershipService.addMember(testOrg.getId(), request);

		assertThat(response).isNotNull();
		assertThat(response.id()).isNotNull();
		assertThat(response.userId()).isEqualTo(target.getId());
		assertThat(response.orgRole()).isEqualTo(OrgRole.ORG_STAFF);
		assertThat(response.status()).isEqualTo(MembershipStatus.ACTIVE);

		OrganizationMembership saved = membershipRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getOrgRole()).isEqualTo(OrgRole.ORG_STAFF);
		assertThat(saved.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
	}

	@Test
	@DisplayName("A2: Add valid active user as ORGANIZATION_ADMIN")
	void addMember_Success_Admin() {
		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORGANIZATION_ADMIN);

		MembershipResponse response = membershipService.addMember(testOrg.getId(), request);

		assertThat(response.orgRole()).isEqualTo(OrgRole.ORGANIZATION_ADMIN);
		assertThat(membershipRepository.countActiveAdmins(testOrg.getId())).isEqualTo(2L);
	}

	@Test
	@DisplayName("A3: Add with unknown loginIdentifier throws UserNotFoundException")
	void addMember_UnknownLoginIdentifier_ThrowsUserNotFoundException() {
		AddMemberRequest request = new AddMemberRequest("nonexistent@example.com", OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(UserNotFoundException.class)
				.hasMessageContaining("nonexistent@example.com");
	}

	@Test
	@DisplayName("A4: Add with unknown organizationId throws OrganizationNotFoundException")
	void addMember_UnknownOrganizationId_ThrowsOrganizationNotFoundException() {
		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(999999L, request))
				.isInstanceOf(OrganizationNotFoundException.class);
	}

	@Test
	@DisplayName("A5: Add user who already has ACTIVE membership throws MembershipAlreadyExistsException")
	void addMember_AlreadyActiveMember_ThrowsMembershipAlreadyExistsException() {
		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		membershipRepository.save(new OrganizationMembership(testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(MembershipAlreadyExistsException.class)
				.satisfies(ex -> assertThat(((MembershipAlreadyExistsException) ex).getExistingStatus())
						.contains(MembershipStatus.ACTIVE));
	}

	@Test
	@DisplayName("A6: Add user who already has SUSPENDED membership throws MembershipAlreadyExistsException")
	void addMember_AlreadySuspendedMember_ThrowsMembershipAlreadyExistsException() {
		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		membershipRepository.save(new OrganizationMembership(testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.SUSPENDED));

		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(MembershipAlreadyExistsException.class)
				.satisfies(ex -> assertThat(((MembershipAlreadyExistsException) ex).getExistingStatus())
						.contains(MembershipStatus.SUSPENDED));
	}

	@Test
	@DisplayName("A7: Add user who already has REVOKED membership throws MembershipAlreadyExistsException")
	void addMember_AlreadyRevokedMember_ThrowsMembershipAlreadyExistsException() {
		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		membershipRepository.save(new OrganizationMembership(testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.REVOKED));

		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(MembershipAlreadyExistsException.class)
				.satisfies(ex -> assertThat(((MembershipAlreadyExistsException) ex).getExistingStatus())
						.contains(MembershipStatus.REVOKED));
	}

	@Test
	@DisplayName("A8: Caller has no membership in organization throws OrganizationAccessDeniedException")
	void addMember_CallerNoMembership_ThrowsOrganizationAccessDeniedException() {
		UserAccount outsider = createUser("outsider-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		authenticateAs(outsider);

		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(OrganizationAccessDeniedException.class)
				.hasMessageContaining("Caller has no membership");
	}

	@Test
	@DisplayName("A9: Caller has SUSPENDED membership throws OrganizationAccessDeniedException")
	void addMember_CallerSuspended_ThrowsOrganizationAccessDeniedException() {
		adminMembership.suspend();
		membershipRepository.save(adminMembership);

		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(OrganizationAccessDeniedException.class)
				.hasMessageContaining("not active");
	}

	@Test
	@DisplayName("A10: Caller has ORG_STAFF role throws OrganizationAccessDeniedException")
	void addMember_CallerStaff_ThrowsOrganizationAccessDeniedException() {
		UserAccount staffUser = createUser("staff-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		membershipRepository.save(new OrganizationMembership(testOrg, staffUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));
		authenticateAs(staffUser);

		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(OrganizationAccessDeniedException.class)
				.hasMessageContaining("Caller does not hold ORGANIZATION_ADMIN");
	}

	@Test
	@DisplayName("A11: Organization is SUSPENDED throws OrganizationAccessDeniedException")
	void addMember_OrgSuspended_ThrowsOrganizationAccessDeniedException() {
		testOrg.suspend();
		organizationRepository.save(testOrg);

		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(OrganizationAccessDeniedException.class)
				.hasMessageContaining("Organization is not active");
	}

	@Test
	@DisplayName("A12: Organization is DEACTIVATED throws OrganizationAccessDeniedException")
	void addMember_OrgDeactivated_ThrowsOrganizationAccessDeniedException() {
		testOrg.deactivate();
		organizationRepository.save(testOrg);

		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(OrganizationAccessDeniedException.class)
				.hasMessageContaining("Organization is not active");
	}

	@Test
	@DisplayName("A13: Target user has AccountStatus.DISABLED throws InactiveUserAccountException")
	void addMember_DisabledTargetUser_ThrowsInactiveUserAccountException() {
		UserAccount target = createUser("disabled-" + System.nanoTime() + "@example.com", AccountStatus.DISABLED);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(InactiveUserAccountException.class)
				.hasMessageContaining("Cannot add inactive user");
	}

	@Test
	@DisplayName("A14: Unrelated DB failure during membership insert propagates DataIntegrityViolationException")
	void addMember_UnrelatedIntegrityFailure_PropagatesUnchanged() {
		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		doThrow(new DataIntegrityViolationException("foreign key violation: fk_other_table"))
				.when(membershipRepository).flush();

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(DataIntegrityViolationException.class)
				.isNotInstanceOf(MembershipAlreadyExistsException.class)
				.hasMessageContaining("fk_other_table");
	}

	@Test
	@DisplayName("A15: Simulated race: uk_org_memberships_org_user unique violation translates to MembershipAlreadyExistsException")
	void addMember_ConcurrentRaceConstraint_TranslatesToDuplicateException() {
		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		doThrow(new DataIntegrityViolationException("Duplicate entry: uk_org_memberships_org_user"))
				.when(membershipRepository).flush();

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(MembershipAlreadyExistsException.class)
				.hasMessageContaining("User already has a membership in this organization");
	}

	// ==========================================
	// Group B: changeRole
	// ==========================================

	@Test
	@DisplayName("B1: Promote ORG_STAFF to ORGANIZATION_ADMIN")
	void changeRole_PromoteStaffToAdmin_Success() {
		UserAccount target = createUser("staff-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		MembershipResponse response = membershipService.changeRole(
				testOrg.getId(), member.getId(), new ChangeMemberRoleRequest(OrgRole.ORGANIZATION_ADMIN));

		assertThat(response.orgRole()).isEqualTo(OrgRole.ORGANIZATION_ADMIN);
		assertThat(membershipRepository.countActiveAdmins(testOrg.getId())).isEqualTo(2L);
	}

	@Test
	@DisplayName("B2: Demote ORGANIZATION_ADMIN to ORG_STAFF when 2 admins exist")
	void changeRole_DemoteAdminWhenMultipleAdmins_Success() {
		UserAccount secondAdmin = createUser("admin2-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member2 = membershipRepository.save(new OrganizationMembership(
				testOrg, secondAdmin, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.ACTIVE));

		MembershipResponse response = membershipService.changeRole(
				testOrg.getId(), member2.getId(), new ChangeMemberRoleRequest(OrgRole.ORG_STAFF));

		assertThat(response.orgRole()).isEqualTo(OrgRole.ORG_STAFF);
		assertThat(membershipRepository.countActiveAdmins(testOrg.getId())).isEqualTo(1L);
	}

	@Test
	@DisplayName("B3: Demote sole remaining ORGANIZATION_ADMIN throws LastAdminProtectionException")
	void changeRole_DemoteSoleAdmin_ThrowsLastAdminProtectionException() {
		assertThatThrownBy(() -> membershipService.changeRole(
				testOrg.getId(), adminMembership.getId(), new ChangeMemberRoleRequest(OrgRole.ORG_STAFF)))
				.isInstanceOf(LastAdminProtectionException.class)
				.hasMessageContaining("Cannot remove or demote the last active administrator");

		OrganizationMembership reloaded = membershipRepository.findById(adminMembership.getId()).orElseThrow();
		assertThat(reloaded.getOrgRole()).isEqualTo(OrgRole.ORGANIZATION_ADMIN);
	}

	@Test
	@DisplayName("B4: Change role of SUSPENDED membership throws InvalidMembershipTransitionException")
	void changeRole_SuspendedMembership_ThrowsInvalidMembershipTransitionException() {
		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.SUSPENDED));

		assertThatThrownBy(() -> membershipService.changeRole(
				testOrg.getId(), member.getId(), new ChangeMemberRoleRequest(OrgRole.ORGANIZATION_ADMIN)))
				.isInstanceOf(InvalidMembershipTransitionException.class)
				.hasMessageContaining("Cannot change role of non-active membership");
	}

	@Test
	@DisplayName("B5: Change role of REVOKED membership throws InvalidMembershipTransitionException")
	void changeRole_RevokedMembership_ThrowsInvalidMembershipTransitionException() {
		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.REVOKED));

		assertThatThrownBy(() -> membershipService.changeRole(
				testOrg.getId(), member.getId(), new ChangeMemberRoleRequest(OrgRole.ORGANIZATION_ADMIN)))
				.isInstanceOf(InvalidMembershipTransitionException.class)
				.hasMessageContaining("Cannot change role of non-active membership");
	}

	@Test
	@DisplayName("B6: Target membership does not exist throws MembershipNotFoundException")
	void changeRole_NonExistentMembership_ThrowsMembershipNotFoundException() {
		assertThatThrownBy(() -> membershipService.changeRole(
				testOrg.getId(), 999999L, new ChangeMemberRoleRequest(OrgRole.ORG_STAFF)))
				.isInstanceOf(MembershipNotFoundException.class);
	}

	@Test
	@DisplayName("B7: Target membership belongs to different organization throws MembershipNotFoundException")
	void changeRole_MembershipBelongsToOtherOrg_ThrowsMembershipNotFoundException() {
		Organization otherOrg = organizationRepository.save(new Organization(
				"Other Org", "other-org-" + System.nanoTime(), OrganizationStatus.ACTIVE));
		UserAccount target = createUser("other-user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership otherMember = membershipRepository.save(new OrganizationMembership(
				otherOrg, target, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		assertThatThrownBy(() -> membershipService.changeRole(
				testOrg.getId(), otherMember.getId(), new ChangeMemberRoleRequest(OrgRole.ORGANIZATION_ADMIN)))
				.isInstanceOf(MembershipNotFoundException.class);
	}

	@Test
	@DisplayName("B8: Caller is not active ORGANIZATION_ADMIN throws OrganizationAccessDeniedException")
	void changeRole_CallerStaff_ThrowsOrganizationAccessDeniedException() {
		UserAccount staffUser = createUser("staff-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership staffMember = membershipRepository.save(new OrganizationMembership(
				testOrg, staffUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));
		authenticateAs(staffUser);

		assertThatThrownBy(() -> membershipService.changeRole(
				testOrg.getId(), adminMembership.getId(), new ChangeMemberRoleRequest(OrgRole.ORG_STAFF)))
				.isInstanceOf(OrganizationAccessDeniedException.class);
	}

	@Test
	@DisplayName("B9: Self-demotion by sole admin throws LastAdminProtectionException")
	void changeRole_SelfDemotionSoleAdmin_ThrowsLastAdminProtectionException() {
		assertThatThrownBy(() -> membershipService.changeRole(
				testOrg.getId(), adminMembership.getId(), new ChangeMemberRoleRequest(OrgRole.ORG_STAFF)))
				.isInstanceOf(LastAdminProtectionException.class);
	}

	// ==========================================
	// Group C: suspendMembership
	// ==========================================

	@Test
	@DisplayName("C1: Suspend ACTIVE ORG_STAFF")
	void suspendMembership_Staff_Success() {
		UserAccount target = createUser("staff-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		MembershipResponse response = membershipService.suspendMembership(testOrg.getId(), member.getId());

		assertThat(response.status()).isEqualTo(MembershipStatus.SUSPENDED);
		OrganizationMembership saved = membershipRepository.findById(member.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(MembershipStatus.SUSPENDED);
	}

	@Test
	@DisplayName("C2: Suspend ACTIVE ORGANIZATION_ADMIN when 2 admins exist")
	void suspendMembership_AdminWhenMultipleAdmins_Success() {
		UserAccount secondAdmin = createUser("admin2-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member2 = membershipRepository.save(new OrganizationMembership(
				testOrg, secondAdmin, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.ACTIVE));

		MembershipResponse response = membershipService.suspendMembership(testOrg.getId(), member2.getId());

		assertThat(response.status()).isEqualTo(MembershipStatus.SUSPENDED);
		assertThat(membershipRepository.countActiveAdmins(testOrg.getId())).isEqualTo(1L);
	}

	@Test
	@DisplayName("C3: Suspend sole remaining active ORGANIZATION_ADMIN throws LastAdminProtectionException")
	void suspendMembership_SoleAdmin_ThrowsLastAdminProtectionException() {
		assertThatThrownBy(() -> membershipService.suspendMembership(testOrg.getId(), adminMembership.getId()))
				.isInstanceOf(LastAdminProtectionException.class);

		OrganizationMembership reloaded = membershipRepository.findById(adminMembership.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
	}

	@Test
	@DisplayName("C4: Suspend already SUSPENDED membership is idempotent")
	void suspendMembership_AlreadySuspended_Idempotent() {
		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.SUSPENDED));

		MembershipResponse response = membershipService.suspendMembership(testOrg.getId(), member.getId());

		assertThat(response.status()).isEqualTo(MembershipStatus.SUSPENDED);
	}

	@Test
	@DisplayName("C5: Suspend REVOKED membership throws InvalidMembershipTransitionException")
	void suspendMembership_Revoked_ThrowsInvalidMembershipTransitionException() {
		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.REVOKED));

		assertThatThrownBy(() -> membershipService.suspendMembership(testOrg.getId(), member.getId()))
				.isInstanceOf(InvalidMembershipTransitionException.class)
				.hasMessageContaining("Cannot suspend a revoked membership");
	}

	@Test
	@DisplayName("C6: Target membership does not exist throws MembershipNotFoundException")
	void suspendMembership_NotFound_ThrowsMembershipNotFoundException() {
		assertThatThrownBy(() -> membershipService.suspendMembership(testOrg.getId(), 999999L))
				.isInstanceOf(MembershipNotFoundException.class);
	}

	@Test
	@DisplayName("C7: Caller is not active ORGANIZATION_ADMIN throws OrganizationAccessDeniedException")
	void suspendMembership_CallerNotAdmin_ThrowsOrganizationAccessDeniedException() {
		UserAccount staffUser = createUser("staff-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		membershipRepository.save(new OrganizationMembership(testOrg, staffUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));
		authenticateAs(staffUser);

		assertThatThrownBy(() -> membershipService.suspendMembership(testOrg.getId(), adminMembership.getId()))
				.isInstanceOf(OrganizationAccessDeniedException.class);
	}

	@Test
	@DisplayName("C8: Self-suspension by sole admin throws LastAdminProtectionException")
	void suspendMembership_SelfSuspensionSoleAdmin_ThrowsLastAdminProtectionException() {
		assertThatThrownBy(() -> membershipService.suspendMembership(testOrg.getId(), adminMembership.getId()))
				.isInstanceOf(LastAdminProtectionException.class);
	}

	// ==========================================
	// Group D: revokeMembership
	// ==========================================

	@Test
	@DisplayName("D1: Revoke ACTIVE ORG_STAFF")
	void revokeMembership_Staff_Success() {
		UserAccount target = createUser("staff-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		MembershipResponse response = membershipService.revokeMembership(testOrg.getId(), member.getId());

		assertThat(response.status()).isEqualTo(MembershipStatus.REVOKED);
		OrganizationMembership saved = membershipRepository.findById(member.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(MembershipStatus.REVOKED);
	}

	@Test
	@DisplayName("D2: Revoke ACTIVE ORGANIZATION_ADMIN when 2 admins exist")
	void revokeMembership_AdminWhenMultipleAdmins_Success() {
		UserAccount secondAdmin = createUser("admin2-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member2 = membershipRepository.save(new OrganizationMembership(
				testOrg, secondAdmin, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.ACTIVE));

		MembershipResponse response = membershipService.revokeMembership(testOrg.getId(), member2.getId());

		assertThat(response.status()).isEqualTo(MembershipStatus.REVOKED);
		assertThat(membershipRepository.countActiveAdmins(testOrg.getId())).isEqualTo(1L);
	}

	@Test
	@DisplayName("D3: Revoke sole remaining active ORGANIZATION_ADMIN throws LastAdminProtectionException")
	void revokeMembership_SoleAdmin_ThrowsLastAdminProtectionException() {
		assertThatThrownBy(() -> membershipService.revokeMembership(testOrg.getId(), adminMembership.getId()))
				.isInstanceOf(LastAdminProtectionException.class);

		OrganizationMembership reloaded = membershipRepository.findById(adminMembership.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
	}

	@Test
	@DisplayName("D4: Revoke SUSPENDED ORGANIZATION_ADMIN succeeds without admin count check")
	void revokeMembership_SuspendedAdmin_Success() {
		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.SUSPENDED));

		MembershipResponse response = membershipService.revokeMembership(testOrg.getId(), member.getId());

		assertThat(response.status()).isEqualTo(MembershipStatus.REVOKED);
	}

	@Test
	@DisplayName("D5: Revoke already REVOKED membership is idempotent")
	void revokeMembership_AlreadyRevoked_Idempotent() {
		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.REVOKED));

		MembershipResponse response = membershipService.revokeMembership(testOrg.getId(), member.getId());

		assertThat(response.status()).isEqualTo(MembershipStatus.REVOKED);
	}

	@Test
	@DisplayName("D6: Target membership does not exist throws MembershipNotFoundException")
	void revokeMembership_NotFound_ThrowsMembershipNotFoundException() {
		assertThatThrownBy(() -> membershipService.revokeMembership(testOrg.getId(), 999999L))
				.isInstanceOf(MembershipNotFoundException.class);
	}

	@Test
	@DisplayName("D7: Caller is not active ORGANIZATION_ADMIN throws OrganizationAccessDeniedException")
	void revokeMembership_CallerNotAdmin_ThrowsOrganizationAccessDeniedException() {
		UserAccount staffUser = createUser("staff-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		membershipRepository.save(new OrganizationMembership(testOrg, staffUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));
		authenticateAs(staffUser);

		assertThatThrownBy(() -> membershipService.revokeMembership(testOrg.getId(), adminMembership.getId()))
				.isInstanceOf(OrganizationAccessDeniedException.class);
	}

	@Test
	@DisplayName("D8: Self-revocation by sole admin throws LastAdminProtectionException")
	void revokeMembership_SelfRevocationSoleAdmin_ThrowsLastAdminProtectionException() {
		assertThatThrownBy(() -> membershipService.revokeMembership(testOrg.getId(), adminMembership.getId()))
				.isInstanceOf(LastAdminProtectionException.class);
	}

	// ==========================================
	// Group E: reactivateMembership
	// ==========================================

	@Test
	@DisplayName("E1: Reactivate SUSPENDED membership as ORG_STAFF")
	void reactivateMembership_Suspended_Success() {
		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.SUSPENDED));

		MembershipResponse response = membershipService.reactivateMembership(
				testOrg.getId(), member.getId(), new ReactivateMemberRequest(OrgRole.ORG_STAFF));

		assertThat(response.status()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(response.orgRole()).isEqualTo(OrgRole.ORG_STAFF);

		OrganizationMembership saved = membershipRepository.findById(member.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(saved.getOrgRole()).isEqualTo(OrgRole.ORG_STAFF);
	}

	@Test
	@DisplayName("E2: Reactivate REVOKED membership as ORGANIZATION_ADMIN")
	void reactivateMembership_Revoked_Success() {
		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.REVOKED));

		MembershipResponse response = membershipService.reactivateMembership(
				testOrg.getId(), member.getId(), new ReactivateMemberRequest(OrgRole.ORGANIZATION_ADMIN));

		assertThat(response.status()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(response.orgRole()).isEqualTo(OrgRole.ORGANIZATION_ADMIN);
		assertThat(membershipRepository.countActiveAdmins(testOrg.getId())).isEqualTo(2L);
	}

	@Test
	@DisplayName("E3: Reactivate REVOKED membership with role change to ORG_STAFF")
	void reactivateMembership_RevokedWithRoleChange_Success() {
		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.REVOKED));

		MembershipResponse response = membershipService.reactivateMembership(
				testOrg.getId(), member.getId(), new ReactivateMemberRequest(OrgRole.ORG_STAFF));

		assertThat(response.status()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(response.orgRole()).isEqualTo(OrgRole.ORG_STAFF);
	}

	@Test
	@DisplayName("E4: Reactivate already ACTIVE membership throws InvalidMembershipTransitionException")
	void reactivateMembership_AlreadyActive_ThrowsInvalidMembershipTransitionException() {
		assertThatThrownBy(() -> membershipService.reactivateMembership(
				testOrg.getId(), adminMembership.getId(), new ReactivateMemberRequest(OrgRole.ORGANIZATION_ADMIN)))
				.isInstanceOf(InvalidMembershipTransitionException.class)
				.hasMessageContaining("Membership is already active");
	}

	@Test
	@DisplayName("E5: Target membership does not exist throws MembershipNotFoundException")
	void reactivateMembership_NotFound_ThrowsMembershipNotFoundException() {
		assertThatThrownBy(() -> membershipService.reactivateMembership(
				testOrg.getId(), 999999L, new ReactivateMemberRequest(OrgRole.ORGANIZATION_ADMIN)))
				.isInstanceOf(MembershipNotFoundException.class);
	}

	@Test
	@DisplayName("E6: Null role supplied in request throws IllegalArgumentException")
	void reactivateMembership_NullRole_ThrowsIllegalArgumentException() {
		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.SUSPENDED));

		assertThatThrownBy(() -> membershipService.reactivateMembership(
				testOrg.getId(), member.getId(), new ReactivateMemberRequest(null)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("E7: Caller is not active ORGANIZATION_ADMIN throws OrganizationAccessDeniedException")
	void reactivateMembership_CallerNotAdmin_ThrowsOrganizationAccessDeniedException() {
		UserAccount staffUser = createUser("staff-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		membershipRepository.save(new OrganizationMembership(testOrg, staffUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));
		authenticateAs(staffUser);

		UserAccount target = createUser("user-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.SUSPENDED));

		assertThatThrownBy(() -> membershipService.reactivateMembership(
				testOrg.getId(), member.getId(), new ReactivateMemberRequest(OrgRole.ORG_STAFF)))
				.isInstanceOf(OrganizationAccessDeniedException.class);
	}

	// ==========================================
	// Group F: Rollback & Transaction Integrity
	// ==========================================

	@Test
	@DisplayName("F1: addMember fails on repository save rolls back transaction")
	void addMember_SaveFailure_RollsBack() {
		UserAccount target = createUser("target-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		doThrow(new DataAccessResourceFailureException("Simulated connection dropped"))
				.when(membershipRepository).save(any(OrganizationMembership.class));

		assertThatThrownBy(() -> membershipService.addMember(testOrg.getId(), request))
				.isInstanceOf(DataAccessResourceFailureException.class);

		Optional<OrganizationMembership> orphan = membershipRepository
				.findByOrganizationIdAndUserId(testOrg.getId(), target.getId());
		assertThat(orphan).isEmpty();
	}

	@Test
	@DisplayName("F2: suspendMembership fails on repository save rolls back transaction")
	void suspendMembership_SaveFailure_RollsBack() {
		UserAccount target = createUser("staff-" + System.nanoTime() + "@example.com", AccountStatus.ACTIVE);
		OrganizationMembership member = membershipRepository.save(new OrganizationMembership(
				testOrg, target, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		doThrow(new DataAccessResourceFailureException("Simulated DB error"))
				.when(membershipRepository).save(any(OrganizationMembership.class));

		assertThatThrownBy(() -> membershipService.suspendMembership(testOrg.getId(), member.getId()))
				.isInstanceOf(DataAccessResourceFailureException.class);

		OrganizationMembership reloaded = membershipRepository.findById(member.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
	}

	@Test
	@DisplayName("F3: Organization lock lookup returns empty throws OrganizationNotFoundException before mutation")
	void lockLookup_Empty_ThrowsOrganizationNotFoundException() {
		assertThatThrownBy(() -> membershipService.suspendMembership(999999L, 1L))
				.isInstanceOf(OrganizationNotFoundException.class);
	}
}
