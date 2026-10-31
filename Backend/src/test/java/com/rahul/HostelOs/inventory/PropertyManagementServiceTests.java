package com.rahul.HostelOs.inventory;

import com.rahul.HostelOs.config.TestDatabaseProfileResolver;
import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.identity.Role;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.RoleRepository;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.inventory.dto.ChangePropertyStatusRequest;
import com.rahul.HostelOs.inventory.dto.CreatePropertyRequest;
import com.rahul.HostelOs.inventory.dto.PropertyResponse;
import com.rahul.HostelOs.inventory.dto.UpdatePropertyRequest;
import com.rahul.HostelOs.inventory.exception.DuplicatePropertyCodeException;
import com.rahul.HostelOs.inventory.exception.InvalidPropertyStatusTransitionException;
import com.rahul.HostelOs.inventory.exception.PropertyNotFoundException;
import com.rahul.HostelOs.organization.MembershipStatus;
import com.rahul.HostelOs.organization.OrgRole;
import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.organization.OrganizationMembership;
import com.rahul.HostelOs.organization.OrganizationMembershipRepository;
import com.rahul.HostelOs.organization.OrganizationRepository;
import com.rahul.HostelOs.organization.OrganizationStatus;
import com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException;
import com.rahul.HostelOs.organization.exception.OrganizationNotFoundException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles(resolver = TestDatabaseProfileResolver.class)
@Transactional
class PropertyManagementServiceTests {

	@Autowired
	private PropertyManagementService propertyService;

	@Autowired
	private PropertyRepository propertyRepository;

	@MockitoSpyBean
	private PropertyRepository propertyRepositorySpy;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private RoleRepository roleRepository;

	private Role staffRole;
	private Organization org1;
	private Organization org2;
	private UserAccount adminUser;
	private UserAccount staffUser;
	private UserAccount alienUser;

	@BeforeEach
	void setUp() {
		reset(propertyRepositorySpy);

		staffRole = roleRepository.findByName(RoleName.STAFF).orElseThrow();

		long suffix = System.nanoTime();
		org1 = organizationRepository.save(new Organization(
				"Test Org One " + suffix,
				"test-org-1-" + suffix,
				OrganizationStatus.ACTIVE));
		org2 = organizationRepository.save(new Organization(
				"Test Org Two " + suffix,
				"test-org-2-" + suffix,
				OrganizationStatus.ACTIVE));

		adminUser = createUser("admin-" + suffix + "@example.com", AccountStatus.ACTIVE);
		staffUser = createUser("staff-" + suffix + "@example.com", AccountStatus.ACTIVE);
		alienUser = createUser("alien-" + suffix + "@example.com", AccountStatus.ACTIVE);

		// Admin membership in Org1
		membershipRepository.save(new OrganizationMembership(
				org1, adminUser, OrgRole.ORG_ADMIN, MembershipStatus.ACTIVE));

		// Staff membership in Org1
		membershipRepository.save(new OrganizationMembership(
				org1, staffUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		// Alien membership in Org2 only
		membershipRepository.save(new OrganizationMembership(
				org2, alienUser, OrgRole.ORG_ADMIN, MembershipStatus.ACTIVE));

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
	// 1. Property Creation
	// ==========================================

	@Test
	@DisplayName("Create property successfully with valid parameters")
	void createProperty_Success() {
		CreatePropertyRequest request = new CreatePropertyRequest(
				"Green Valley Hostel",
				"gvh-01",
				"Plot 42, North Campus Road",
				"Near Gate 2",
				"Bengaluru",
				"Karnataka",
				"560001",
				"IN",
				"Asia/Kolkata");

		PropertyResponse response = propertyService.createProperty(org1.getId(), request);

		assertThat(response).isNotNull();
		assertThat(response.id()).isNotNull();
		assertThat(response.organizationId()).isEqualTo(org1.getId());
		assertThat(response.name()).isEqualTo("Green Valley Hostel");
		assertThat(response.code()).isEqualTo("GVH-01");
		assertThat(response.status()).isEqualTo(PropertyStatus.ACTIVE);
		assertThat(response.city()).isEqualTo("Bengaluru");
		assertThat(response.country()).isEqualTo("IN");
		assertThat(response.timezone()).isEqualTo("Asia/Kolkata");
		assertThat(response.version()).isZero();
		assertThat(response.createdAt()).isNotNull();
		assertThat(response.updatedAt()).isNotNull();
	}

	@Test
	@DisplayName("Create property rejects duplicate code in same organization")
	void createProperty_DuplicateCode_Rejected() {
		CreatePropertyRequest request1 = new CreatePropertyRequest(
				"Hostel A", "PROP-1", null, null, "City", null, null, null, null);
		propertyService.createProperty(org1.getId(), request1);

		CreatePropertyRequest request2 = new CreatePropertyRequest(
				"Hostel B", "prop-1", null, null, "City", null, null, null, null);

		assertThatThrownBy(() -> propertyService.createProperty(org1.getId(), request2))
				.isInstanceOf(DuplicatePropertyCodeException.class)
				.hasMessageContaining("PROP-1");
	}

	@Test
	@DisplayName("Create property allows duplicate code across different organizations")
	void createProperty_SameCodeDifferentOrganizations_Allowed() {
		CreatePropertyRequest request1 = new CreatePropertyRequest(
				"Hostel Org1", "COMMON-CODE", null, null, "City", null, null, null, null);
		PropertyResponse response1 = propertyService.createProperty(org1.getId(), request1);

		authenticateAs(alienUser);
		CreatePropertyRequest request2 = new CreatePropertyRequest(
				"Hostel Org2", "common-code", null, null, "City", null, null, null, null);
		PropertyResponse response2 = propertyService.createProperty(org2.getId(), request2);

		assertThat(response1.code()).isEqualTo("COMMON-CODE");
		assertThat(response2.code()).isEqualTo("COMMON-CODE");
		assertThat(response1.organizationId()).isEqualTo(org1.getId());
		assertThat(response2.organizationId()).isEqualTo(org2.getId());
	}

	@Test
	@DisplayName("Create property rejects unauthenticated caller")
	void createProperty_Unauthenticated_Rejected() {
		SecurityContextHolder.clearContext();
		CreatePropertyRequest request = new CreatePropertyRequest(
				"Hostel", "CODE-1", null, null, "City", null, null, null, null);

		assertThatThrownBy(() -> propertyService.createProperty(org1.getId(), request))
				.isInstanceOf(OrganizationAccessDeniedException.class)
				.hasMessageContaining("Unauthenticated");
	}

	@Test
	@DisplayName("Create property rejects non-member caller")
	void createProperty_NonMember_Rejected() {
		authenticateAs(alienUser);
		CreatePropertyRequest request = new CreatePropertyRequest(
				"Hostel", "CODE-1", null, null, "City", null, null, null, null);

		assertThatThrownBy(() -> propertyService.createProperty(org1.getId(), request))
				.isInstanceOf(OrganizationAccessDeniedException.class)
				.hasMessageContaining("Caller has no membership");
	}

	@Test
	@DisplayName("Create property rejects ORG_STAFF caller")
	void createProperty_OrgStaff_Rejected() {
		authenticateAs(staffUser);
		CreatePropertyRequest request = new CreatePropertyRequest(
				"Hostel", "CODE-1", null, null, "City", null, null, null, null);

		assertThatThrownBy(() -> propertyService.createProperty(org1.getId(), request))
				.isInstanceOf(OrganizationAccessDeniedException.class)
				.hasMessageContaining("ORG_ADMIN");
	}

	@Test
	@DisplayName("Create property rejects non-existent organization")
	void createProperty_NonExistentOrg_Rejected() {
		CreatePropertyRequest request = new CreatePropertyRequest(
				"Hostel", "CODE-1", null, null, "City", null, null, null, null);

		assertThatThrownBy(() -> propertyService.createProperty(999999L, request))
				.isInstanceOf(OrganizationNotFoundException.class);
	}

	@Test
	@DisplayName("Create property rejects inactive organization")
	void createProperty_InactiveOrg_Rejected() {
		org1.suspend();
		organizationRepository.saveAndFlush(org1);

		CreatePropertyRequest request = new CreatePropertyRequest(
				"Hostel", "CODE-1", null, null, "City", null, null, null, null);

		assertThatThrownBy(() -> propertyService.createProperty(org1.getId(), request))
				.isInstanceOf(OrganizationAccessDeniedException.class)
				.hasMessageContaining("Organization is not active");
	}

	// ==========================================
	// 2. Property Update
	// ==========================================

	@Test
	@DisplayName("Update property successfully replaces mutable configuration fields")
	void updateProperty_Success() {
		PropertyResponse created = propertyService.createProperty(org1.getId(), new CreatePropertyRequest(
				"Original Name", "ORIG-01", "Old Addr 1", "Old Addr 2", "Old City", "Old State", "100000", "IN", "Asia/Kolkata"));

		UpdatePropertyRequest updateRequest = new UpdatePropertyRequest(
				"New Name", "New Addr 1", "New Addr 2", "New City", "New State", "200000", "IN", "Asia/Kolkata");

		PropertyResponse updated = propertyService.updateProperty(org1.getId(), created.id(), updateRequest);

		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(updated.organizationId()).isEqualTo(org1.getId());
		assertThat(updated.code()).isEqualTo("ORIG-01"); // Code immutable
		assertThat(updated.name()).isEqualTo("New Name");
		assertThat(updated.addressLine1()).isEqualTo("New Addr 1");
		assertThat(updated.addressLine2()).isEqualTo("New Addr 2");
		assertThat(updated.city()).isEqualTo("New City");
		assertThat(updated.state()).isEqualTo("New State");
		assertThat(updated.postalCode()).isEqualTo("200000");
	}

	@Test
	@DisplayName("Update property isolates across tenants")
	void updateProperty_CrossTenant_Rejected() {
		PropertyResponse created = propertyService.createProperty(org1.getId(), new CreatePropertyRequest(
				"Org1 Hostel", "TENANT-1", null, null, "City", null, null, null, null));

		authenticateAs(alienUser);
		UpdatePropertyRequest updateRequest = new UpdatePropertyRequest(
				"Hacked Name", null, null, "City", null, null, null, null);

		assertThatThrownBy(() -> propertyService.updateProperty(org2.getId(), created.id(), updateRequest))
				.isInstanceOf(PropertyNotFoundException.class);
	}

	@Test
	@DisplayName("Update property rejects modifications on archived property")
	void updateProperty_ArchivedProperty_Rejected() {
		PropertyResponse created = propertyService.createProperty(org1.getId(), new CreatePropertyRequest(
				"Archived Hostel", "ARCH-01", null, null, "City", null, null, null, null));

		propertyService.changeStatus(org1.getId(), created.id(), new ChangePropertyStatusRequest(PropertyStatus.ARCHIVED));

		UpdatePropertyRequest updateRequest = new UpdatePropertyRequest(
				"Attempted Name", null, null, "City", null, null, null, null);

		assertThatThrownBy(() -> propertyService.updateProperty(org1.getId(), created.id(), updateRequest))
				.isInstanceOf(InvalidPropertyStatusTransitionException.class)
				.hasMessageContaining("archived");
	}

	@Test
	@DisplayName("Update property rejects ORG_STAFF caller")
	void updateProperty_OrgStaff_Rejected() {
		PropertyResponse created = propertyService.createProperty(org1.getId(), new CreatePropertyRequest(
				"Hostel", "STAFF-UPD", null, null, "City", null, null, null, null));

		authenticateAs(staffUser);
		UpdatePropertyRequest updateRequest = new UpdatePropertyRequest(
				"New Name", null, null, "City", null, null, null, null);

		assertThatThrownBy(() -> propertyService.updateProperty(org1.getId(), created.id(), updateRequest))
				.isInstanceOf(OrganizationAccessDeniedException.class)
				.hasMessageContaining("ORG_ADMIN");
	}

	// ==========================================
	// 3. Status Transitions
	// ==========================================

	@Test
	@DisplayName("Status transition: ACTIVE -> INACTIVE -> ACTIVE -> ARCHIVED")
	void changeStatus_ValidTransitions() {
		PropertyResponse created = propertyService.createProperty(org1.getId(), new CreatePropertyRequest(
				"Lifecycle Hostel", "LIFE-01", null, null, "City", null, null, null, null));
		assertThat(created.status()).isEqualTo(PropertyStatus.ACTIVE);

		// ACTIVE -> INACTIVE (suspend)
		PropertyResponse suspended = propertyService.changeStatus(
				org1.getId(), created.id(), new ChangePropertyStatusRequest(PropertyStatus.INACTIVE));
		assertThat(suspended.status()).isEqualTo(PropertyStatus.INACTIVE);

		// INACTIVE -> ACTIVE (reactivate)
		PropertyResponse activated = propertyService.changeStatus(
				org1.getId(), created.id(), new ChangePropertyStatusRequest(PropertyStatus.ACTIVE));
		assertThat(activated.status()).isEqualTo(PropertyStatus.ACTIVE);

		// ACTIVE -> ARCHIVED (decommission)
		PropertyResponse archived = propertyService.changeStatus(
				org1.getId(), created.id(), new ChangePropertyStatusRequest(PropertyStatus.ARCHIVED));
		assertThat(archived.status()).isEqualTo(PropertyStatus.ARCHIVED);
	}

	@Test
	@DisplayName("Status transition: cannot transition out of ARCHIVED")
	void changeStatus_FromArchived_Rejected() {
		PropertyResponse created = propertyService.createProperty(org1.getId(), new CreatePropertyRequest(
				"Terminal Hostel", "TERM-01", null, null, "City", null, null, null, null));

		propertyService.changeStatus(org1.getId(), created.id(), new ChangePropertyStatusRequest(PropertyStatus.ARCHIVED));

		// Cannot transition ARCHIVED -> ACTIVE
		assertThatThrownBy(() -> propertyService.changeStatus(
				org1.getId(), created.id(), new ChangePropertyStatusRequest(PropertyStatus.ACTIVE)))
				.isInstanceOf(InvalidPropertyStatusTransitionException.class)
				.hasMessageContaining("ARCHIVED");

		// Cannot transition ARCHIVED -> INACTIVE
		assertThatThrownBy(() -> propertyService.changeStatus(
				org1.getId(), created.id(), new ChangePropertyStatusRequest(PropertyStatus.INACTIVE)))
				.isInstanceOf(InvalidPropertyStatusTransitionException.class)
				.hasMessageContaining("ARCHIVED");
	}

	// ==========================================
	// 4. Reads & Tenant Isolation
	// ==========================================

	@Test
	@DisplayName("Read property allowed for both ORG_ADMIN and ORG_STAFF")
	void getProperty_AdminAndStaffAllowed() {
		PropertyResponse created = propertyService.createProperty(org1.getId(), new CreatePropertyRequest(
				"Read Hostel", "READ-01", null, null, "City", null, null, null, null));

		// Admin reads
		PropertyResponse adminRead = propertyService.getProperty(org1.getId(), created.id());
		assertThat(adminRead.id()).isEqualTo(created.id());

		// Staff reads
		authenticateAs(staffUser);
		PropertyResponse staffRead = propertyService.getProperty(org1.getId(), created.id());
		assertThat(staffRead.id()).isEqualTo(created.id());
	}

	@Test
	@DisplayName("List properties with and without status filter")
	void listProperties_FiltersCorrectly() {
		PropertyResponse prop1 = propertyService.createProperty(org1.getId(), new CreatePropertyRequest(
				"Active 1", "ACT-01", null, null, "City", null, null, null, null));
		PropertyResponse prop2 = propertyService.createProperty(org1.getId(), new CreatePropertyRequest(
				"Active 2", "ACT-02", null, null, "City", null, null, null, null));
		propertyService.changeStatus(org1.getId(), prop2.id(), new ChangePropertyStatusRequest(PropertyStatus.INACTIVE));

		List<PropertyResponse> allProps = propertyService.listProperties(org1.getId(), null);
		assertThat(allProps).hasSize(2);

		List<PropertyResponse> activeProps = propertyService.listProperties(org1.getId(), PropertyStatus.ACTIVE);
		assertThat(activeProps).hasSize(1);
		assertThat(activeProps.get(0).code()).isEqualTo("ACT-01");

		List<PropertyResponse> inactiveProps = propertyService.listProperties(org1.getId(), PropertyStatus.INACTIVE);
		assertThat(inactiveProps).hasSize(1);
		assertThat(inactiveProps.get(0).code()).isEqualTo("ACT-02");
	}

	@Test
	@DisplayName("Get property rejects cross-tenant access")
	void getProperty_CrossTenant_Rejected() {
		PropertyResponse created = propertyService.createProperty(org1.getId(), new CreatePropertyRequest(
				"Isolated Hostel", "ISO-01", null, null, "City", null, null, null, null));

		authenticateAs(alienUser);
		assertThatThrownBy(() -> propertyService.getProperty(org2.getId(), created.id()))
				.isInstanceOf(PropertyNotFoundException.class);
	}

	// ==========================================
	// 5. Database Constraint Error Translation
	// ==========================================

	@Test
	@DisplayName("Translates uk_properties_org_code DataIntegrityViolationException into DuplicatePropertyCodeException")
	void databaseConstraintViolationTranslation_DuplicateCode() {
		CreatePropertyRequest request = new CreatePropertyRequest(
				"Hostel Flush", "FLUSH-01", null, null, "City", null, null, null, null);

		// Simulate concurrent DB conflict emitting uk_properties_org_code on save
		org.hibernate.exception.ConstraintViolationException hibernateCve =
				new org.hibernate.exception.ConstraintViolationException(
						"Duplicate entry for uk_properties_org_code", null, "uk_properties_org_code");
		DataIntegrityViolationException dive = new DataIntegrityViolationException("Conflict", hibernateCve);

		doThrow(dive).when(propertyRepositorySpy).flush();

		assertThatThrownBy(() -> propertyService.createProperty(org1.getId(), request))
				.isInstanceOf(DuplicatePropertyCodeException.class)
				.hasMessageContaining("FLUSH-01");
	}

	@Test
	@DisplayName("Propagates unrelated DataIntegrityViolationException without translation")
	void databaseConstraintViolation_Unrelated_Propagates() {
		CreatePropertyRequest request = new CreatePropertyRequest(
				"Hostel Other", "OTHER-01", null, null, "City", null, null, null, null);

		DataIntegrityViolationException dive = new DataIntegrityViolationException("Unrelated integrity violation");
		doThrow(dive).when(propertyRepositorySpy).flush();

		assertThatThrownBy(() -> propertyService.createProperty(org1.getId(), request))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("Unrelated integrity violation");
	}
}
