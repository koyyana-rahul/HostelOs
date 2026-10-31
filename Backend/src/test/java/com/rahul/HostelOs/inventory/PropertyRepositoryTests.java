package com.rahul.HostelOs.inventory;

import com.rahul.HostelOs.config.TestDatabaseProfileResolver;
import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.organization.OrganizationRepository;
import com.rahul.HostelOs.organization.OrganizationStatus;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
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
@ActiveProfiles(resolver = TestDatabaseProfileResolver.class)
@Transactional
class PropertyRepositoryTests {

	@Autowired
	private PropertyRepository propertyRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private EntityManager entityManager;

	private Organization organization1;
	private Organization organization2;

	@BeforeEach
	void setUp() {
		long suffix = System.nanoTime();
		organization1 = organizationRepository.save(
				new Organization("Org One", "org-one-" + suffix, OrganizationStatus.ACTIVE));
		organization2 = organizationRepository.save(
				new Organization("Org Two", "org-two-" + suffix, OrganizationStatus.ACTIVE));
	}

	@Test
	void propertyPersistsWithOrganizationAndGeneratedValues() {
		Property property = new Property(
				organization1,
				"Green Valley Hostel",
				"gvh-01",
				PropertyStatus.ACTIVE,
				"Plot 42, North Campus Road",
				"Near University Gate",
				"Bengaluru",
				"Karnataka",
				"560001",
				"IN",
				"Asia/Kolkata");

		Property saved = propertyRepository.saveAndFlush(property);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getOrganization().getId()).isEqualTo(organization1.getId());
		assertThat(saved.getName()).isEqualTo("Green Valley Hostel");
		assertThat(saved.getCode()).isEqualTo("GVH-01");
		assertThat(saved.getStatus()).isEqualTo(PropertyStatus.ACTIVE);
		assertThat(saved.getCity()).isEqualTo("Bengaluru");
		assertThat(saved.getCountry()).isEqualTo("IN");
		assertThat(saved.getTimezone()).isEqualTo("Asia/Kolkata");
		assertThat(saved.getVersion()).isZero();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();

		Optional<Property> found = propertyRepository.findByOrganizationIdAndId(organization1.getId(), saved.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getCode()).isEqualTo("GVH-01");
	}

	@Test
	void codeNormalizationAndValidationAreEnforced() {
		Property property = new Property(
				organization1,
				"Tech Residency",
				"  tech_park-north  ",
				PropertyStatus.ACTIVE,
				null,
				null,
				"Hyderabad",
				"Telangana",
				"500081",
				"IN",
				"Asia/Kolkata");

		Property saved = propertyRepository.saveAndFlush(property);
		assertThat(saved.getCode()).isEqualTo("TECH_PARK-NORTH");

		// Code length < 2 rejected
		assertThatThrownBy(() -> new Property(organization1, "Invalid", "A", PropertyStatus.ACTIVE, null, null, "City", null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class);

		// Code length > 30 rejected
		assertThatThrownBy(() -> new Property(organization1, "Invalid", "A".repeat(31), PropertyStatus.ACTIVE, null, null, "City", null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class);

		// Code with invalid characters (@, space, etc.) rejected
		assertThatThrownBy(() -> new Property(organization1, "Invalid", "GVH@NORTH", PropertyStatus.ACTIVE, null, null, "City", null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void duplicatePropertyCodeInSameOrganizationIsRejected() {
		Property prop1 = new Property(
				organization1,
				"Campus Main 1",
				"MAIN",
				PropertyStatus.ACTIVE,
				null,
				null,
				"Bengaluru",
				null,
				null,
				null,
				null);
		propertyRepository.saveAndFlush(prop1);

		Property prop2 = new Property(
				organization1,
				"Campus Main 2",
				"main",
				PropertyStatus.ACTIVE,
				null,
				null,
				"Bengaluru",
				null,
				null,
				null,
				null);

		assertThatThrownBy(() -> propertyRepository.saveAndFlush(prop2))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void samePropertyCodeInDifferentOrganizationsIsAllowed() {
		Property prop1 = new Property(
				organization1,
				"Org1 Main Hostel",
				"MAIN",
				PropertyStatus.ACTIVE,
				null,
				null,
				"Bengaluru",
				null,
				null,
				null,
				null);
		propertyRepository.saveAndFlush(prop1);

		Property prop2 = new Property(
				organization2,
				"Org2 Main Hostel",
				"MAIN",
				PropertyStatus.ACTIVE,
				null,
				null,
				"Chennai",
				null,
				null,
				null,
				null);
		Property saved2 = propertyRepository.saveAndFlush(prop2);

		assertThat(saved2.getId()).isNotNull();
		assertThat(propertyRepository.findByOrganizationIdAndCode(organization1.getId(), "MAIN")).isPresent();
		assertThat(propertyRepository.findByOrganizationIdAndCode(organization2.getId(), "MAIN")).isPresent();
	}

	@Test
	void tenantScopedQueriesIsolatePropertiesByOrganization() {
		Property prop1 = propertyRepository.saveAndFlush(new Property(
				organization1, "Org1 Hostel", "H1", PropertyStatus.ACTIVE, null, null, "City", null, null, null, null));
		Property prop2 = propertyRepository.saveAndFlush(new Property(
				organization2, "Org2 Hostel", "H2", PropertyStatus.ACTIVE, null, null, "City", null, null, null, null));

		// Org1 cannot retrieve Org2 property
		assertThat(propertyRepository.findByOrganizationIdAndId(organization1.getId(), prop2.getId())).isEmpty();
		assertThat(propertyRepository.findByOrganizationIdAndId(organization2.getId(), prop1.getId())).isEmpty();

		// Lookup by code strictly isolates by tenant
		assertThat(propertyRepository.findByOrganizationIdAndCode(organization1.getId(), "H2")).isEmpty();
		assertThat(propertyRepository.findByOrganizationIdAndCode(organization2.getId(), "H1")).isEmpty();

		// Listing strictly scoped
		List<Property> org1Properties = propertyRepository.findByOrganizationId(organization1.getId());
		assertThat(org1Properties).hasSize(1);
		assertThat(org1Properties.get(0).getId()).isEqualTo(prop1.getId());

		List<Property> org2Properties = propertyRepository.findByOrganizationId(organization2.getId());
		assertThat(org2Properties).hasSize(1);
		assertThat(org2Properties.get(0).getId()).isEqualTo(prop2.getId());

		// Exists queries isolated
		assertThat(propertyRepository.existsByOrganizationIdAndCode(organization1.getId(), "H1")).isTrue();
		assertThat(propertyRepository.existsByOrganizationIdAndCode(organization2.getId(), "H1")).isFalse();
		assertThat(propertyRepository.existsByOrganizationIdAndId(organization1.getId(), prop1.getId())).isTrue();
		assertThat(propertyRepository.existsByOrganizationIdAndId(organization2.getId(), prop1.getId())).isFalse();
	}

	@Test
	void organizationDeletionRestrictedWhenPropertiesExist() {
		propertyRepository.saveAndFlush(new Property(
				organization1, "Active Hostel", "ACTIVE-1", PropertyStatus.ACTIVE, null, null, "City", null, null, null, null));

		// Attempting to delete organization1 must fail at the database level due to ON DELETE RESTRICT foreign key constraint
		assertThatThrownBy(() -> {
			entityManager.createNativeQuery("DELETE FROM organizations WHERE id = :id")
					.setParameter("id", organization1.getId())
					.executeUpdate();
		})
				.isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class)
				.hasMessageMatching("(?is).*(fk_properties_organization|foreign key|referential integrity).*");
	}

	@Test
	void pessimisticLockQueryRetrievesPropertyWithOrganization() {
		Property prop = propertyRepository.saveAndFlush(new Property(
				organization1, "Locked Hostel", "LOCK-1", PropertyStatus.ACTIVE, null, null, "City", null, null, null, null));

		Optional<Property> locked = propertyRepository.findByIdAndOrganizationIdWithLock(prop.getId(), organization1.getId());
		assertThat(locked).isPresent();
		assertThat(locked.get().getOrganization().getId()).isEqualTo(organization1.getId());

		// Searching with wrong organization returns empty
		Optional<Property> wrongOrg = propertyRepository.findByIdAndOrganizationIdWithLock(prop.getId(), organization2.getId());
		assertThat(wrongOrg).isEmpty();
	}

	@Test
	void lifecycleTransitionsFollowStateRules() {
		Property prop = propertyRepository.saveAndFlush(new Property(
				organization1, "Lifecycle Hostel", "LIFE-1", PropertyStatus.ACTIVE, null, null, "City", null, null, null, null));

		// ACTIVE -> INACTIVE (Suspend)
		prop.suspend();
		propertyRepository.saveAndFlush(prop);
		assertThat(prop.getStatus()).isEqualTo(PropertyStatus.INACTIVE);

		// INACTIVE -> ACTIVE (Reactivate)
		prop.activate();
		propertyRepository.saveAndFlush(prop);
		assertThat(prop.getStatus()).isEqualTo(PropertyStatus.ACTIVE);

		// ACTIVE -> ARCHIVED (Decommission)
		prop.archive();
		propertyRepository.saveAndFlush(prop);
		assertThat(prop.getStatus()).isEqualTo(PropertyStatus.ARCHIVED);

		// ARCHIVED is terminal -> suspend or activate must throw IllegalStateException
		assertThatThrownBy(prop::suspend).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(prop::activate).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> prop.updateDetails("New Name", null, null, "City", null, null, null, null))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void updateDetailsModifiesConfigurationFields() {
		Property prop = propertyRepository.saveAndFlush(new Property(
				organization1, "Original Name", "UPD-1", PropertyStatus.ACTIVE, "Addr 1", null, "Old City", null, null, null, null));

		prop.updateDetails(
				"New Name",
				"New Addr 1",
				"New Addr 2",
				"New City",
				"Karnataka",
				"560002",
				"IN",
				"Asia/Kolkata");
		Property updated = propertyRepository.saveAndFlush(prop);

		assertThat(updated.getName()).isEqualTo("New Name");
		assertThat(updated.getAddressLine1()).isEqualTo("New Addr 1");
		assertThat(updated.getAddressLine2()).isEqualTo("New Addr 2");
		assertThat(updated.getCity()).isEqualTo("New City");
		assertThat(updated.getState()).isEqualTo("Karnataka");
		assertThat(updated.getPostalCode()).isEqualTo("560002");
	}
}
