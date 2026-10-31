package com.rahul.HostelOs.concurrency;

import com.rahul.HostelOs.config.TestDatabaseProfileResolver;
import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.Role;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.RoleRepository;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.organization.MembershipManagementService;
import com.rahul.HostelOs.organization.MembershipStatus;
import com.rahul.HostelOs.organization.OrgRole;
import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.organization.OrganizationMembership;
import com.rahul.HostelOs.organization.OrganizationMembershipRepository;
import com.rahul.HostelOs.organization.OrganizationProvisioningService;
import com.rahul.HostelOs.organization.OrganizationRepository;
import com.rahul.HostelOs.organization.dto.AddMemberRequest;
import com.rahul.HostelOs.organization.dto.ChangeMemberRoleRequest;
import com.rahul.HostelOs.organization.dto.CreateOrganizationRequest;
import com.rahul.HostelOs.organization.dto.MembershipResponse;
import com.rahul.HostelOs.organization.dto.OrganizationResponse;
import com.rahul.HostelOs.organization.exception.DuplicateOrganizationSlugException;
import com.rahul.HostelOs.organization.exception.LastAdminProtectionException;
import com.rahul.HostelOs.organization.exception.MembershipAlreadyExistsException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles(resolver = TestDatabaseProfileResolver.class)
@EnabledIf(ConcurrencyTestSupport.MYSQL_TEST_CONDITION)
@Timeout(30)
class OrganizationConcurrencyIntegrationTests {

	private static final String TEST_PASSWORD_HASH =
			"$2a$12$01234567890123456789012345678901234567890123456789012";

	@Autowired
	private OrganizationProvisioningService provisioningService;

	@Autowired
	private MembershipManagementService membershipService;

	@MockitoSpyBean
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Environment environment;

	private ConcurrencyTestSupport support;
	private ConcurrencyTestSupport.DatabaseSnapshot baseline;
	private Role staffRole;

	@BeforeEach
	void setUp() {
		ConcurrencyTestSupport.assumeMysqlTestProfile(environment);
		ConcurrencyTestSupport.requireNoActiveTransaction();
		SecurityContextHolder.clearContext();
		reset(organizationRepository);
		staffRole = roleRepository.findByName(RoleName.STAFF).orElseThrow();
		support = new ConcurrencyTestSupport(jdbcTemplate);
		baseline = support.snapshot();
	}

	@AfterEach
	void tearDown() {
		try {
			if (support != null && baseline != null) {
				support.cleanup();
				assertThat(support.snapshot()).isEqualTo(baseline);
			}
		} finally {
			SecurityContextHolder.clearContext();
			reset(organizationRepository);
		}
	}

	@Test
	@DisplayName("F2F-01: concurrent organization provisioning with the same slug")
	void f2f01_concurrentOrganizationProvisioningWithSameSlug() throws Exception {
		UserAccount creator = createUser("p2f-creator");
		String slug = "p2f-org-" + System.nanoTime();
		CreateOrganizationRequest request = new CreateOrganizationRequest("P2F Organization", slug);

		ConcurrencyTestSupport.DualCaptureGate gate =
				new ConcurrencyTestSupport.DualCaptureGate();
		doAnswer(gate.answer()).when(organizationRepository).existsBySlug(slug);
		doAnswer(gate.answer()).when(organizationRepository).existsBySlug(slug);

		ExecutorService executor = ConcurrencyTestSupport.newExecutor();
		try {
			Future<OrganizationResponse> first = executor.submit(ConcurrencyTestSupport.authenticated(
					creator, () -> provisioningService.provisionOrganization(request, creator.getId())));
			Future<OrganizationResponse> second = executor.submit(ConcurrencyTestSupport.authenticated(
					creator, () -> provisioningService.provisionOrganization(request, creator.getId())));

			// The CyclicBarrier inside DualCaptureGate blocks each worker until
			// both have executed their existsBySlug query; after the barrier the
			// test asserts that both results were false.
			gate.assertBothFalse();

			List<ConcurrencyTestSupport.Outcome<OrganizationResponse>> outcomes =
					ConcurrencyTestSupport.awaitAll(List.of(first, second));
			List<OrganizationResponse> successes = outcomes.stream()
					.filter(ConcurrencyTestSupport.Outcome::succeeded)
					.map(ConcurrencyTestSupport.Outcome::value)
					.toList();
			for (OrganizationResponse success : successes) {
				support.trackOrganization(success.id());
				OrganizationMembership membership = findMembership(success.id(), creator.getId());
				support.trackMembership(membership.getId());
			}

			ConcurrencyTestSupport.assertExactlyOneSuccessAndOneFailure(
					outcomes, DuplicateOrganizationSlugException.class);

			OrganizationResponse winner = successes.get(0);
			OrganizationMembership membership = findMembership(winner.id(), creator.getId());
			assertThat(support.count("SELECT COUNT(*) FROM organizations WHERE slug = ?", slug)).isEqualTo(1L);
			assertThat(support.count(
					"SELECT COUNT(*) FROM organization_memberships WHERE organization_id = ? AND user_id = ?",
					winner.id(), creator.getId())).isEqualTo(1L);
			assertThat(organizationRepository.findById(winner.id()).orElseThrow().getVersion()).isZero();
			assertThat(membership.getVersion()).isZero();
			assertThat(membership.getOrgRole()).isEqualTo(OrgRole.ORG_ADMIN);
			assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		} finally {
			ConcurrencyTestSupport.shutdown(executor);
		}
	}

	@Test
	@DisplayName("F2F-02: concurrent addition of the same user to one organization")
	void f2f02_concurrentMembershipAdditionForSameUser() throws Exception {
		UserAccount admin = createUser("p2f-membership-admin");
		UserAccount target = createUser("p2f-membership-target");
		OrganizationResponse organization = createOrganization(admin, "p2f-membership");
		AddMemberRequest request = new AddMemberRequest(target.getLoginIdentifier(), OrgRole.ORG_STAFF);

		ConcurrencyTestSupport.OrganizationLockGate gate = new ConcurrencyTestSupport.OrganizationLockGate();
		doAnswer(gate.answer()).when(organizationRepository).findByIdWithLock(anyLong());

		ExecutorService executor = ConcurrencyTestSupport.newExecutor();
		try {
			Future<MembershipResponse> first = executor.submit(ConcurrencyTestSupport.authenticated(
					admin, () -> membershipService.addMember(organization.id(), request)));
			gate.awaitFirstLocked();
			Future<MembershipResponse> second = executor.submit(ConcurrencyTestSupport.authenticated(
					admin, () -> membershipService.addMember(organization.id(), request)));
			gate.awaitSecondAttempted();
			ConcurrencyTestSupport.assertBlocked(second);
			gate.releaseFirst();

			List<ConcurrencyTestSupport.Outcome<MembershipResponse>> outcomes =
					ConcurrencyTestSupport.awaitAll(List.of(first, second));
			for (ConcurrencyTestSupport.Outcome<MembershipResponse> outcome : outcomes) {
				if (outcome.succeeded()) {
					support.trackMembership(outcome.value().id());
				}
			}

			ConcurrencyTestSupport.assertExactlyOneSuccessAndOneFailure(
					outcomes, MembershipAlreadyExistsException.class);

			OrganizationMembership targetMembership = findMembership(organization.id(), target.getId());
			support.trackMembership(targetMembership.getId());
			OrganizationMembership adminMembership = findMembership(organization.id(), admin.getId());
			assertThat(support.count(
					"SELECT COUNT(*) FROM organization_memberships WHERE organization_id = ? AND user_id = ?",
					organization.id(), target.getId())).isEqualTo(1L);
			assertThat(targetMembership.getVersion()).isZero();
			assertThat(targetMembership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
			assertThat(adminMembership.getVersion()).isZero();
			assertThat(membershipRepository.countActiveAdmins(organization.id())).isEqualTo(1L);
		} finally {
			gate.releaseFirst();
			ConcurrencyTestSupport.shutdown(executor);
		}
	}

	@Test
	@DisplayName("F2F-03: concurrent demotion attempts for the last two active admins")
	void f2f03_concurrentLastAdminDemotion() throws Exception {
		UserAccount adminA = createUser("p2f-last-admin-a");
		UserAccount adminB = createUser("p2f-last-admin-b");
		OrganizationResponse organization = createOrganization(adminA, "p2f-last-admin");
		OrganizationMembership membershipA = findMembership(organization.id(), adminA.getId());
		Organization organizationEntity = organizationRepository.findById(organization.id()).orElseThrow();
		OrganizationMembership membershipB = membershipRepository.save(new OrganizationMembership(
				organizationEntity, adminB, OrgRole.ORG_ADMIN, MembershipStatus.ACTIVE));
		support.trackMembership(membershipB.getId());
		assertThat(membershipA.getVersion()).isZero();
		assertThat(membershipB.getVersion()).isZero();

		ConcurrencyTestSupport.OrganizationLockGate gate = new ConcurrencyTestSupport.OrganizationLockGate();
		doAnswer(gate.answer()).when(organizationRepository).findByIdWithLock(anyLong());

		ExecutorService executor = ConcurrencyTestSupport.newExecutor();
		try {
			Future<MembershipResponse> first = executor.submit(ConcurrencyTestSupport.authenticated(
					adminA, () -> membershipService.changeRole(
							organization.id(), membershipB.getId(), new ChangeMemberRoleRequest(OrgRole.ORG_STAFF))));
			gate.awaitFirstLocked();
			Future<MembershipResponse> second = executor.submit(ConcurrencyTestSupport.authenticated(
					adminA, () -> membershipService.changeRole(
							organization.id(), membershipA.getId(), new ChangeMemberRoleRequest(OrgRole.ORG_STAFF))));
			gate.awaitSecondAttempted();
			ConcurrencyTestSupport.assertBlocked(second);
			gate.releaseFirst();

			List<ConcurrencyTestSupport.Outcome<MembershipResponse>> outcomes =
					ConcurrencyTestSupport.awaitAll(List.of(first, second));
			ConcurrencyTestSupport.assertExactlyOneSuccessAndOneFailure(
					outcomes, LastAdminProtectionException.class);

			OrganizationMembership reloadedA = membershipRepository.findById(membershipA.getId()).orElseThrow();
			OrganizationMembership reloadedB = membershipRepository.findById(membershipB.getId()).orElseThrow();
			assertThat(reloadedA.getOrgRole()).isEqualTo(OrgRole.ORG_ADMIN);
			assertThat(reloadedA.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
			assertThat(reloadedA.getVersion()).isZero();
			assertThat(reloadedB.getOrgRole()).isEqualTo(OrgRole.ORG_STAFF);
			assertThat(reloadedB.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
			assertThat(reloadedB.getVersion()).isEqualTo(1L);
			assertThat(membershipRepository.countActiveAdmins(organization.id())).isEqualTo(1L);
		} finally {
			gate.releaseFirst();
			ConcurrencyTestSupport.shutdown(executor);
		}
	}

	private UserAccount createUser(String prefix) {
		String login = prefix + "-" + System.nanoTime() + "@example.com";
		UserAccount user = userAccountRepository.save(new UserAccount(
				login,
				"P2F " + prefix,
				TEST_PASSWORD_HASH,
				AccountStatus.ACTIVE,
				Set.of(staffRole)));
		support.trackUser(user.getId());
		return user;
	}

	private OrganizationResponse createOrganization(UserAccount creator, String prefix) {
		String slug = prefix + "-" + System.nanoTime();
		OrganizationResponse organization = provisioningService.provisionOrganization(
				new CreateOrganizationRequest("P2F " + prefix, slug),
				creator.getId());
		support.trackOrganization(organization.id());
		OrganizationMembership membership = findMembership(organization.id(), creator.getId());
		support.trackMembership(membership.getId());
		return organization;
	}

	private OrganizationMembership findMembership(Long organizationId, Long userId) {
		return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId).orElseThrow();
	}
}
