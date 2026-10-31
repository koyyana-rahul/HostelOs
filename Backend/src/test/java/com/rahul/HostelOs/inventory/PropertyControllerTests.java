package com.rahul.HostelOs.inventory;

import com.rahul.HostelOs.config.TestDatabaseProfileResolver;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountService;
import com.rahul.HostelOs.organization.MembershipStatus;
import com.rahul.HostelOs.organization.OrgRole;
import com.rahul.HostelOs.organization.Organization;
import com.rahul.HostelOs.organization.OrganizationMembership;
import com.rahul.HostelOs.organization.OrganizationMembershipRepository;
import com.rahul.HostelOs.organization.OrganizationRepository;
import com.rahul.HostelOs.organization.OrganizationStatus;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(resolver = TestDatabaseProfileResolver.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropertyControllerTests {

	private static final String DEFAULT_PASSWORD = "correct-horse-battery-staple";

	@LocalServerPort
	private int port;

	@Autowired
	private UserAccountService userAccounts;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	private Long org1Id;
	private Long org2Id;

	private String adminLogin;
	private String staffLogin;
	private String alienLogin;

	@BeforeAll
	void setUpFixtures() {
		long suffix = System.nanoTime();
		adminLogin = "prop-admin-" + suffix + "@example.com";
		staffLogin = "prop-staff-" + suffix + "@example.com";
		alienLogin = "prop-alien-" + suffix + "@example.com";

		UserAccount adminUser = userAccounts.createUser(
				adminLogin, "Property Admin", DEFAULT_PASSWORD, Set.of(RoleName.STAFF));
		UserAccount staffUser = userAccounts.createUser(
				staffLogin, "Property Staff", DEFAULT_PASSWORD, Set.of(RoleName.STAFF));
		UserAccount alienUser = userAccounts.createUser(
				alienLogin, "Alien Admin", DEFAULT_PASSWORD, Set.of(RoleName.STAFF));

		Organization org1 = organizationRepository.save(new Organization(
				"Controller Org One " + suffix,
				"ctrl-org-1-" + suffix,
				OrganizationStatus.ACTIVE));
		Organization org2 = organizationRepository.save(new Organization(
				"Controller Org Two " + suffix,
				"ctrl-org-2-" + suffix,
				OrganizationStatus.ACTIVE));

		org1Id = org1.getId();
		org2Id = org2.getId();

		// Admin in Org 1
		membershipRepository.save(new OrganizationMembership(
				org1, adminUser, OrgRole.ORG_ADMIN, MembershipStatus.ACTIVE));

		// Staff in Org 1
		membershipRepository.save(new OrganizationMembership(
				org1, staffUser, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		// Admin in Org 2
		membershipRepository.save(new OrganizationMembership(
				org2, alienUser, OrgRole.ORG_ADMIN, MembershipStatus.ACTIVE));
	}

	// ==========================================
	// 1. Successful CRUD & Lifecycle Operations
	// ==========================================

	@Test
	@DisplayName("1. Successful create, list, get, full update, and status change")
	void fullPropertyLifecycle_Success() throws Exception {
		Browser admin = new Browser();
		admin.login(adminLogin, DEFAULT_PASSWORD);

		// 1. Create property
		String createPayload = """
				{
					"name": "Sunrise Campus Hostel",
					"code": "SCH-01",
					"addressLine1": "100 University Ave",
					"addressLine2": "Block A",
					"city": "Bengaluru",
					"state": "Karnataka",
					"postalCode": "560001",
					"country": "IN",
					"timezone": "Asia/Kolkata"
				}
				""";
		HttpResponse<String> createRes = admin.postJson(
				"/api/v1/organizations/" + org1Id + "/properties", createPayload);
		assertThat(createRes.statusCode()).isEqualTo(201);
		assertThat(createRes.body())
				.contains("\"name\":\"Sunrise Campus Hostel\"")
				.contains("\"code\":\"SCH-01\"")
				.contains("\"status\":\"ACTIVE\"");

		String propertyId = jsonField(createRes.body(), "id");

		// 2. Get property
		HttpResponse<String> getRes = admin.get(
				"/api/v1/organizations/" + org1Id + "/properties/" + propertyId);
		assertThat(getRes.statusCode()).isEqualTo(200);
		assertThat(getRes.body()).contains("\"name\":\"Sunrise Campus Hostel\"");

		// 3. List properties
		HttpResponse<String> listRes = admin.get(
				"/api/v1/organizations/" + org1Id + "/properties");
		assertThat(listRes.statusCode()).isEqualTo(200);
		assertThat(listRes.body()).contains("\"code\":\"SCH-01\"");

		// 4. Update property (full replacement of mutable fields)
		String updatePayload = """
				{
					"name": "Sunrise Campus Hostel Renamed",
					"addressLine1": "200 University Ave",
					"addressLine2": "Block B",
					"city": "Bengaluru",
					"state": "Karnataka",
					"postalCode": "560002",
					"country": "IN",
					"timezone": "Asia/Kolkata"
				}
				""";
		HttpResponse<String> updateRes = admin.putJson(
				"/api/v1/organizations/" + org1Id + "/properties/" + propertyId, updatePayload);
		assertThat(updateRes.statusCode()).isEqualTo(200);
		assertThat(updateRes.body())
				.contains("\"name\":\"Sunrise Campus Hostel Renamed\"")
				.contains("\"postalCode\":\"560002\"")
				.contains("\"code\":\"SCH-01\""); // Code preserved immutable

		// 5. Change status (PATCH status to INACTIVE)
		String suspendPayload = "{\"status\":\"INACTIVE\"}";
		HttpResponse<String> patchRes = admin.patchJson(
				"/api/v1/organizations/" + org1Id + "/properties/" + propertyId + "/status", suspendPayload);
		assertThat(patchRes.statusCode()).isEqualTo(200);
		assertThat(patchRes.body()).contains("\"status\":\"INACTIVE\"");

		// 6. List with filter
		HttpResponse<String> filterRes = admin.get(
				"/api/v1/organizations/" + org1Id + "/properties?status=INACTIVE");
		assertThat(filterRes.statusCode()).isEqualTo(200);
		assertThat(filterRes.body()).contains("\"status\":\"INACTIVE\"");
	}

	// ==========================================
	// 2. Validation Failures (400)
	// ==========================================

	@Test
	@DisplayName("2. 400 Bad Request on invalid request bodies")
	void validationErrors_Return400() throws Exception {
		Browser admin = new Browser();
		admin.login(adminLogin, DEFAULT_PASSWORD);

		// Invalid: blank name and invalid code characters
		String invalidPayload = """
				{
					"name": "   ",
					"code": "INVALID@CODE#",
					"city": ""
				}
				""";
		HttpResponse<String> response = admin.postJson(
				"/api/v1/organizations/" + org1Id + "/properties", invalidPayload);

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body())
				.contains("\"status\":400")
				.contains("\"violations\"");
	}

	// ==========================================
	// 3. Unauthenticated Access (401)
	// ==========================================

	@Test
	@DisplayName("3. 401 Unauthorized for unauthenticated requests")
	void unauthenticatedRequests_Return401() throws Exception {
		Browser unauthenticated = new Browser();

		// GET list unauthenticated
		HttpResponse<String> getRes = unauthenticated.get(
				"/api/v1/organizations/" + org1Id + "/properties");
		assertThat(getRes.statusCode()).isEqualTo(401);
		assertThat(getRes.body()).contains("\"status\":401");

		// POST create unauthenticated
		HttpResponse<String> postRes = unauthenticated.postJson(
				"/api/v1/organizations/" + org1Id + "/properties", "{}");
		assertThat(postRes.statusCode()).isEqualTo(401);
		assertThat(postRes.body()).contains("\"status\":401");
	}

	// ==========================================
	// 4. CSRF Protection and Role Authorization (403)
	// ==========================================

	@Test
	@DisplayName("4a. 403 Forbidden when CSRF header is missing on protected mutations")
	void missingCsrf_Returns403() throws Exception {
		Browser admin = new Browser();
		admin.login(adminLogin, DEFAULT_PASSWORD);

		String payload = """
				{
					"name": "CSRF Test Hostel",
					"code": "CSRF-01",
					"city": "Bengaluru"
				}
				""";
		HttpResponse<String> response = admin.postWithoutCsrf(
				"/api/v1/organizations/" + org1Id + "/properties", payload);

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"status\":403");
	}

	@Test
	@DisplayName("4b. 403 Forbidden when ORG_STAFF attempts mutations (ORG_ADMIN required)")
	void staffAttemptingMutations_Returns403() throws Exception {
		Browser staff = new Browser();
		staff.login(staffLogin, DEFAULT_PASSWORD);

		// Staff can read
		HttpResponse<String> readRes = staff.get(
				"/api/v1/organizations/" + org1Id + "/properties");
		assertThat(readRes.statusCode()).isEqualTo(200);

		// Staff cannot create
		String createPayload = """
				{
					"name": "Staff Attempt",
					"code": "STAFF-01",
					"city": "Bengaluru"
				}
				""";
		HttpResponse<String> createRes = staff.postJson(
				"/api/v1/organizations/" + org1Id + "/properties", createPayload);
		assertThat(createRes.statusCode()).isEqualTo(403);
		assertThat(createRes.body()).contains("\"status\":403");
	}

	@Test
	@DisplayName("4c. 403 Forbidden when caller has no membership in organization")
	void crossTenantCaller_Returns403() throws Exception {
		Browser alien = new Browser();
		alien.login(alienLogin, DEFAULT_PASSWORD);

		// Alien belongs to Org 2, attempts access to Org 1
		HttpResponse<String> response = alien.get(
				"/api/v1/organizations/" + org1Id + "/properties");
		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"status\":403");
	}

	// ==========================================
	// 5. Not Found & Cross-Tenant Property Isolation (404)
	// ==========================================

	@Test
	@DisplayName("5. 404 Not Found for missing property or property in another organization")
	void missingOrCrossTenantProperty_Returns404() throws Exception {
		Browser admin = new Browser();
		admin.login(adminLogin, DEFAULT_PASSWORD);

		// Non-existent property ID
		HttpResponse<String> missingRes = admin.get(
				"/api/v1/organizations/" + org1Id + "/properties/999999");
		assertThat(missingRes.statusCode()).isEqualTo(404);
		assertThat(missingRes.body())
				.contains("\"status\":404")
				.contains("999999 not found in organization");

		// Create property in Org 2
		Browser alien = new Browser();
		alien.login(alienLogin, DEFAULT_PASSWORD);
		HttpResponse<String> org2PropRes = alien.postJson(
				"/api/v1/organizations/" + org2Id + "/properties",
				"""
				{
					"name": "Org 2 Property",
					"code": "ORG2-01",
					"city": "Chennai"
				}
				""");
		assertThat(org2PropRes.statusCode()).isEqualTo(201);
		String org2PropId = jsonField(org2PropRes.body(), "id");

		// Admin of Org 1 attempts to query Org 2's property ID under Org 1's path -> 404
		HttpResponse<String> crossRes = admin.get(
				"/api/v1/organizations/" + org1Id + "/properties/" + org2PropId);
		assertThat(crossRes.statusCode()).isEqualTo(404);
		assertThat(crossRes.body()).contains("\"status\":404");
	}

	// ==========================================
	// 6. Conflict Errors (409)
	// ==========================================

	@Test
	@DisplayName("6a. 409 Conflict on duplicate property code within same organization")
	void duplicatePropertyCode_Returns409() throws Exception {
		Browser admin = new Browser();
		admin.login(adminLogin, DEFAULT_PASSWORD);

		String payload1 = """
				{
					"name": "Alpha Hostel",
					"code": "DUP-CODE",
					"city": "Bengaluru"
				}
				""";
		HttpResponse<String> res1 = admin.postJson(
				"/api/v1/organizations/" + org1Id + "/properties", payload1);
		assertThat(res1.statusCode()).isEqualTo(201);

		String payload2 = """
				{
					"name": "Alpha Hostel Second",
					"code": "dup-code",
					"city": "Bengaluru"
				}
				""";
		HttpResponse<String> res2 = admin.postJson(
				"/api/v1/organizations/" + org1Id + "/properties", payload2);
		assertThat(res2.statusCode()).isEqualTo(409);
		assertThat(res2.body())
				.contains("\"status\":409")
				.contains("already exists");
	}

	@Test
	@DisplayName("6b. 409 Conflict on invalid lifecycle state transition")
	void invalidLifecycleTransition_Returns409() throws Exception {
		Browser admin = new Browser();
		admin.login(adminLogin, DEFAULT_PASSWORD);

		// Create property
		HttpResponse<String> createRes = admin.postJson(
				"/api/v1/organizations/" + org1Id + "/properties",
				"""
				{
					"name": "Decommissioned Hostel",
					"code": "DECOM-01",
					"city": "Bengaluru"
				}
				""");
		String propertyId = jsonField(createRes.body(), "id");

		// Decommission to ARCHIVED
		HttpResponse<String> archiveRes = admin.patchJson(
				"/api/v1/organizations/" + org1Id + "/properties/" + propertyId + "/status",
				"{\"status\":\"ARCHIVED\"}");
		assertThat(archiveRes.statusCode()).isEqualTo(200);
		assertThat(archiveRes.body()).contains("\"status\":\"ARCHIVED\"");

		// Attempt transition ARCHIVED -> ACTIVE -> must return 409
		HttpResponse<String> reactivateRes = admin.patchJson(
				"/api/v1/organizations/" + org1Id + "/properties/" + propertyId + "/status",
				"{\"status\":\"ACTIVE\"}");
		assertThat(reactivateRes.statusCode()).isEqualTo(409);
		assertThat(reactivateRes.body())
				.contains("\"status\":409")
				.contains("Cannot transition property status from ARCHIVED to ACTIVE");
	}

	// ==========================================
	// 7. Default Deny Behavior
	// ==========================================

	@Test
	@DisplayName("7. Default-deny behavior rejects requests to undefined routes")
	void defaultDeny_UndefinedRoute_ReturnsDenied() throws Exception {
		Browser admin = new Browser();
		admin.login(adminLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = admin.get("/api/v1/unrelated/route");
		// Unrelated routes fall through to .anyRequest().denyAll() which returns 403
		assertThat(response.statusCode()).isEqualTo(403);
	}

	// ==========================================
	// HTTP Client Helper
	// ==========================================

	private URI baseUri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	private String jsonField(String body, String field) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9]+))").matcher(body);
		assertThat(matcher.find()).as("Field %s in %s", field, body).isTrue();
		return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
	}

	private final class Browser {

		private final CookieManager cookieManager = new CookieManager();
		private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();
		private String csrfHeader = "X-XSRF-TOKEN";
		private String csrfToken;

		HttpResponse<String> bootstrapCsrf() throws Exception {
			HttpResponse<String> response = get("/api/v1/auth/csrf");
			csrfHeader = jsonField(response.body(), "headerName");
			csrfToken = jsonField(response.body(), "token");
			return response;
		}

		HttpResponse<String> login(String login, String password) throws Exception {
			if (csrfToken == null) {
				bootstrapCsrf();
			}
			String body = "{\"loginIdentifier\":\"" + login + "\",\"password\":\"" + password + "\"}";
			return postJson("/api/v1/auth/login", body);
		}

		HttpResponse<String> get(String path) throws Exception {
			HttpRequest request = HttpRequest.newBuilder(baseUri(path))
					.GET()
					.build();
			return client.send(request, HttpResponse.BodyHandlers.ofString());
		}

		HttpResponse<String> postJson(String path, String body) throws Exception {
			return send("POST", path, body, csrfToken);
		}

		HttpResponse<String> putJson(String path, String body) throws Exception {
			return send("PUT", path, body, csrfToken);
		}

		HttpResponse<String> patchJson(String path, String body) throws Exception {
			return send("PATCH", path, body, csrfToken);
		}

		HttpResponse<String> postWithoutCsrf(String path, String body) throws Exception {
			return send("POST", path, body, null);
		}

		private HttpResponse<String> send(String method, String path, String body, String token) throws Exception {
			HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri(path));
			builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
			if (token != null) {
				builder.header(csrfHeader, token);
			}
			builder.method(method, HttpRequest.BodyPublishers.ofString(body));
			return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		}
	}
}
