package com.rahul.HostelOs.concurrency;

import com.rahul.HostelOs.config.TestDatabaseProfileResolver;
import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.Role;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.RoleRepository;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.inventory.Property;
import com.rahul.HostelOs.inventory.PropertyManagementService;
import com.rahul.HostelOs.inventory.PropertyRepository;
import com.rahul.HostelOs.inventory.PropertyStatus;
import com.rahul.HostelOs.inventory.dto.ChangePropertyStatusRequest;
import com.rahul.HostelOs.inventory.dto.CreatePropertyRequest;
import com.rahul.HostelOs.inventory.dto.PropertyResponse;
import com.rahul.HostelOs.inventory.exception.DuplicatePropertyCodeException;
import com.rahul.HostelOs.organization.OrganizationMembership;
import com.rahul.HostelOs.organization.OrganizationMembershipRepository;
import com.rahul.HostelOs.organization.OrganizationProvisioningService;
import com.rahul.HostelOs.organization.OrganizationRepository;
import com.rahul.HostelOs.organization.dto.CreateOrganizationRequest;
import com.rahul.HostelOs.organization.dto.OrganizationResponse;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
class PropertyConcurrencyIntegrationTests {

	private static final String TEST_PASSWORD_HASH =
			"$2a$12$01234567890123456789012345678901234567890123456789012";

	@Autowired
	private PropertyManagementService propertyService;

	@Autowired
	private PropertyRepository propertyRepository;

	@Autowired
	private OrganizationProvisioningService provisioningService;

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
	@DisplayName("F2F-04: concurrent creation of the same property code in one organization")
	void f2f04_concurrentDuplicatePropertyCodeInOneOrganization() throws Exception {
		UserAccount admin = createUser("p2f-property-admin");
		OrganizationResponse organization = createOrganization(admin, "p2f-property");
		String code = "P2F-" + System.nanoTime();
		CreatePropertyRequest request = new CreatePropertyRequest(
				"P2F Hostel", code, null, null, "Bengaluru", null, null, null, null);

		ConcurrencyTestSupport.OrganizationLockGate gate = new ConcurrencyTestSupport.OrganizationLockGate();
		doAnswer(gate.answer()).when(organizationRepository).findByIdWithLock(anyLong());

		ExecutorService executor = ConcurrencyTestSupport.newExecutor();
		try {
			Future<PropertyResponse> first = executor.submit(ConcurrencyTestSupport.authenticated(
					admin, () -> propertyService.createProperty(organization.id(), request)));
			gate.awaitFirstLocked();
			Future<PropertyResponse> second = executor.submit(ConcurrencyTestSupport.authenticated(
					admin, () -> propertyService.createProperty(organization.id(), request)));
			gate.awaitSecondAttempted();
			ConcurrencyTestSupport.assertBlocked(second);
			gate.releaseFirst();

			List<ConcurrencyTestSupport.Outcome<PropertyResponse>> outcomes =
					ConcurrencyTestSupport.awaitAll(List.of(first, second));
			List<PropertyResponse> successes = outcomes.stream()
					.filter(ConcurrencyTestSupport.Outcome::succeeded)
					.map(ConcurrencyTestSupport.Outcome::value)
					.toList();
			for (PropertyResponse success : successes) {
				support.trackProperty(success.id());
			}

			ConcurrencyTestSupport.assertExactlyOneSuccessAndOneFailure(
					outcomes, DuplicatePropertyCodeException.class);

			PropertyResponse winner = successes.get(0);
			Property property = propertyRepository.findById(winner.id()).orElseThrow();
			assertThat(support.count(
					"SELECT COUNT(*) FROM properties WHERE organization_id = ? AND code = ?",
					organization.id(), winner.code())).isEqualTo(1L);
			assertThat(property.getCode()).isEqualTo(winner.code());
			assertThat(property.getStatus()).isEqualTo(PropertyStatus.ACTIVE);
			assertThat(property.getVersion()).isZero();
		} finally {
			gate.releaseFirst();
			ConcurrencyTestSupport.shutdown(executor);
		}
	}

	@Test
	@DisplayName("F2F-05: concurrent property status transitions")
	void f2f05_concurrentPropertyStatusTransitions() throws Exception {
		UserAccount admin = createUser("p2f-status-admin");
		OrganizationResponse organization = createOrganization(admin, "p2f-status");
		String code = "P2F-STATUS-" + System.nanoTime();
		PropertyResponse created = ConcurrencyTestSupport.callAs(admin, () -> propertyService.createProperty(
				organization.id(),
				new CreatePropertyRequest("P2F Status Hostel", code, null, null, "Bengaluru", null, null, null, null)));
		support.trackProperty(created.id());
		assertThat(created.version()).isZero();

		ConcurrencyTestSupport.OrganizationLockGate gate = new ConcurrencyTestSupport.OrganizationLockGate();
		doAnswer(gate.answer()).when(organizationRepository).findByIdWithLock(anyLong());

		ExecutorService executor = ConcurrencyTestSupport.newExecutor();
		try {
			Future<PropertyResponse> first = executor.submit(ConcurrencyTestSupport.authenticated(
					admin, () -> propertyService.changeStatus(
							organization.id(), created.id(),
							new ChangePropertyStatusRequest(PropertyStatus.INACTIVE))));
			gate.awaitFirstLocked();
			Future<PropertyResponse> second = executor.submit(ConcurrencyTestSupport.authenticated(
					admin, () -> propertyService.changeStatus(
							organization.id(), created.id(),
							new ChangePropertyStatusRequest(PropertyStatus.ARCHIVED))));
			gate.awaitSecondAttempted();
			ConcurrencyTestSupport.assertBlocked(second);
			gate.releaseFirst();

			List<ConcurrencyTestSupport.Outcome<PropertyResponse>> outcomes =
					ConcurrencyTestSupport.awaitAll(List.of(first, second));
			assertThat(outcomes).allMatch(ConcurrencyTestSupport.Outcome::succeeded);
			PropertyResponse firstResponse = outcomes.get(0).value();
			PropertyResponse secondResponse = outcomes.get(1).value();
			assertThat(firstResponse.status()).isEqualTo(PropertyStatus.INACTIVE);
			assertThat(firstResponse.version()).isEqualTo(1L);
			assertThat(secondResponse.status()).isEqualTo(PropertyStatus.ARCHIVED);
			assertThat(secondResponse.version()).isEqualTo(2L);

			Property property = propertyRepository.findById(created.id()).orElseThrow();
			assertThat(property.getStatus()).isEqualTo(PropertyStatus.ARCHIVED);
			assertThat(property.getVersion()).isEqualTo(2L);
			assertThat(support.count(
					"SELECT COUNT(*) FROM properties WHERE organization_id = ? AND code = ?",
					organization.id(), property.getCode())).isEqualTo(1L);
		} finally {
			gate.releaseFirst();
			ConcurrencyTestSupport.shutdown(executor);
		}
	}

	@Test
	@DisplayName("F2F-06: same property code created in two different organizations")
	void f2f06_samePropertyCodeAcrossOrganizations() throws Exception {
		UserAccount admin = createUser("p2f-cross-org-admin");
		OrganizationResponse firstOrganization = createOrganization(admin, "p2f-cross-org-a");
		OrganizationResponse secondOrganization = createOrganization(admin, "p2f-cross-org-b");
		String code = "P2F-CROSS-" + System.nanoTime();
		CreatePropertyRequest firstRequest = new CreatePropertyRequest(
				"P2F Cross A", code, null, null, "Bengaluru", null, null, null, null);
		CreatePropertyRequest secondRequest = new CreatePropertyRequest(
				"P2F Cross B", code, null, null, "Bengaluru", null, null, null, null);

		ExecutorService executor = ConcurrencyTestSupport.newExecutor();
		try {
			List<Callable<PropertyResponse>> tasks = List.of(
					ConcurrencyTestSupport.authenticated(
							admin, () -> propertyService.createProperty(firstOrganization.id(), firstRequest)),
					ConcurrencyTestSupport.authenticated(
							admin, () -> propertyService.createProperty(secondOrganization.id(), secondRequest)));
			List<ConcurrencyTestSupport.Outcome<PropertyResponse>> outcomes =
					ConcurrencyTestSupport.runTwo(executor, tasks);
			List<PropertyResponse> successes = outcomes.stream()
					.filter(ConcurrencyTestSupport.Outcome::succeeded)
					.map(ConcurrencyTestSupport.Outcome::value)
					.toList();
			successes.forEach(success -> support.trackProperty(success.id()));

			assertThat(outcomes).allMatch(ConcurrencyTestSupport.Outcome::succeeded);
			assertThat(successes).hasSize(2);
			assertThat(support.count(
					"SELECT COUNT(*) FROM properties WHERE code = ?", code)).isEqualTo(2L);
			for (PropertyResponse success : successes) {
				Property property = propertyRepository.findById(success.id()).orElseThrow();
				assertThat(property.getCode()).isEqualTo(code);
				assertThat(property.getStatus()).isEqualTo(PropertyStatus.ACTIVE);
				assertThat(property.getVersion()).isZero();
				assertThat(support.count(
						"SELECT COUNT(*) FROM properties WHERE organization_id = ? AND code = ?",
						success.organizationId(), code)).isEqualTo(1L);
			}
		} finally {
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
		OrganizationMembership membership = membershipRepository
				.findByOrganizationIdAndUserId(organization.id(), creator.getId())
				.orElseThrow();
		support.trackMembership(membership.getId());
		return organization;
	}
}
