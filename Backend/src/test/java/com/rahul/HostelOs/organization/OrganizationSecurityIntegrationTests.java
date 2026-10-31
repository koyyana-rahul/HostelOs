package com.rahul.HostelOs.organization;

import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.Role;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.RoleRepository;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.identity.UserAccountService;
import com.rahul.HostelOs.security.ApiSecurityErrorHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(OrganizationSecurityIntegrationTests.ProbeConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganizationSecurityIntegrationTests {

	private static final String DEFAULT_PASSWORD = "correct-horse-battery-staple";

	@LocalServerPort
	private int port;

	@Autowired
	private UserAccountService userAccounts;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	private Long orgAlphaId;
	private Long orgBetaId;
	private Long orgSuspendedId;
	private Long orgDeactivatedId;

	private String alphaStaffLogin;
	private String alphaAdminLogin;
	private String betaStaffLogin;
	private String platformAdminLogin;
	private String orgAdminGlobalLogin;

	@BeforeAll
	void setUpFixtures() {
		alphaStaffLogin = "alpha-staff-" + System.nanoTime() + "@example.com";
		alphaAdminLogin = "alpha-admin-" + System.nanoTime() + "@example.com";
		betaStaffLogin = "beta-staff-" + System.nanoTime() + "@example.com";
		platformAdminLogin = "platform-admin-" + System.nanoTime() + "@example.com";
		orgAdminGlobalLogin = "org-admin-global-" + System.nanoTime() + "@example.com";

		UserAccount alphaStaff = userAccounts.createUser(
				alphaStaffLogin, "Alpha Staff", DEFAULT_PASSWORD, Set.of(RoleName.STAFF));
		UserAccount alphaAdmin = userAccounts.createUser(
				alphaAdminLogin, "Alpha Admin", DEFAULT_PASSWORD, Set.of(RoleName.STAFF));
		UserAccount betaStaff = userAccounts.createUser(
				betaStaffLogin, "Beta Staff", DEFAULT_PASSWORD, Set.of(RoleName.STAFF));
		userAccounts.createUser(
				platformAdminLogin, "Platform Admin", DEFAULT_PASSWORD, Set.of(RoleName.PLATFORM_ADMIN));
		userAccounts.createUser(
				orgAdminGlobalLogin, "Global Org Admin", DEFAULT_PASSWORD, Set.of(RoleName.ORGANIZATION_ADMIN));

		long now = System.nanoTime();
		Organization orgAlpha = organizationRepository.save(
				new Organization("Alpha Organization", "alpha-org-" + now, OrganizationStatus.ACTIVE));
		Organization orgBeta = organizationRepository.save(
				new Organization("Beta Organization", "beta-org-" + now, OrganizationStatus.ACTIVE));
		Organization orgSuspended = organizationRepository.save(
				new Organization("Suspended Organization", "suspended-org-" + now, OrganizationStatus.SUSPENDED));
		Organization orgDeactivated = organizationRepository.save(
				new Organization("Deactivated Organization", "deactivated-org-" + now, OrganizationStatus.DEACTIVATED));

		orgAlphaId = orgAlpha.getId();
		orgBetaId = orgBeta.getId();
		orgSuspendedId = orgSuspended.getId();
		orgDeactivatedId = orgDeactivated.getId();

		membershipRepository.save(new OrganizationMembership(
				orgAlpha, alphaStaff, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));
		membershipRepository.save(new OrganizationMembership(
				orgAlpha, alphaAdmin, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.ACTIVE));
		membershipRepository.save(new OrganizationMembership(
				orgBeta, betaStaff, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));
	}

	@Test
	@DisplayName("Anonymous caller without session cookie receives 401 Unauthorized")
	void anonymousRequest_Returns401() throws Exception {
		Browser browser = new Browser();

		HttpResponse<String> response = browser.get("/_test/organizations/" + orgAlphaId + "/staff");

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.body()).contains("\"status\":401");
		assertThat(response.body()).contains("Authentication is required");
	}

	@Test
	@DisplayName("Active staff member accessing staff endpoint in their organization receives 200 OK")
	void staffMember_AccessingStaffEndpoint_Returns200() throws Exception {
		Browser browser = new Browser();
		browser.login(alphaStaffLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = browser.get("/_test/organizations/" + orgAlphaId + "/staff");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"status\":\"ok\"");
		assertThat(response.body()).contains("\"level\":\"staff\"");
	}

	@Test
	@DisplayName("Active admin member accessing admin endpoint in their organization receives 200 OK")
	void adminMember_AccessingAdminEndpoint_Returns200() throws Exception {
		Browser browser = new Browser();
		browser.login(alphaAdminLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = browser.get("/_test/organizations/" + orgAlphaId + "/admin");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"status\":\"ok\"");
		assertThat(response.body()).contains("\"level\":\"admin\"");
	}

	@Test
	@DisplayName("Active admin member accessing staff endpoint in their organization receives 200 OK (role hierarchy)")
	void adminMember_AccessingStaffEndpoint_Returns200() throws Exception {
		Browser browser = new Browser();
		browser.login(alphaAdminLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = browser.get("/_test/organizations/" + orgAlphaId + "/staff");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"status\":\"ok\"");
	}

	@Test
	@DisplayName("Staff member attempting admin-only operation receives generic 403 Forbidden")
	void staffMember_AttemptingAdminEndpoint_Returns403() throws Exception {
		Browser browser = new Browser();
		browser.login(alphaStaffLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = browser.get("/_test/organizations/" + orgAlphaId + "/admin");

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"status\":403");
		assertThat(response.body()).contains("Access is denied");
	}

	@Test
	@DisplayName("Cross-tenant tampering: Member of Org Alpha attempting access to Org Beta receives 403 Forbidden")
	void crossTenantAccess_Returns403() throws Exception {
		Browser browser = new Browser();
		browser.login(alphaStaffLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = browser.get("/_test/organizations/" + orgBetaId + "/staff");

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"status\":403");
		assertThat(response.body()).contains("Access is denied");
	}

	@Test
	@DisplayName("Non-existent organization ID receives identical generic 403 Forbidden (anti-enumeration)")
	void nonExistentOrganization_Returns403WithoutDisclosingExistence() throws Exception {
		Browser browser = new Browser();
		browser.login(alphaStaffLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = browser.get("/_test/organizations/999999/staff");

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"status\":403");
		assertThat(response.body()).contains("Access is denied");
	}

	@Test
	@DisplayName("Access to suspended organization receives 403 Forbidden")
	void accessSuspendedOrganization_Returns403() throws Exception {
		String suspendedStaffLogin = "suspended-staff-" + System.nanoTime() + "@example.com";
		UserAccount suspendedStaff = userAccounts.createUser(
				suspendedStaffLogin, "Suspended Staff", DEFAULT_PASSWORD, Set.of(RoleName.STAFF));
		Organization suspendedOrg = organizationRepository.findById(orgSuspendedId).orElseThrow();
		membershipRepository.save(new OrganizationMembership(
				suspendedOrg, suspendedStaff, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		Browser browser = new Browser();
		browser.login(suspendedStaffLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = browser.get("/_test/organizations/" + orgSuspendedId + "/staff");

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"status\":403");
		assertThat(response.body()).contains("Access is denied");
	}

	@Test
	@DisplayName("Access to deactivated organization receives 403 Forbidden")
	void accessDeactivatedOrganization_Returns403() throws Exception {
		String deactivatedStaffLogin = "deactivated-staff-" + System.nanoTime() + "@example.com";
		UserAccount deactivatedStaff = userAccounts.createUser(
				deactivatedStaffLogin, "Deactivated Staff", DEFAULT_PASSWORD, Set.of(RoleName.STAFF));
		Organization deactivatedOrg = organizationRepository.findById(orgDeactivatedId).orElseThrow();
		membershipRepository.save(new OrganizationMembership(
				deactivatedOrg, deactivatedStaff, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		Browser browser = new Browser();
		browser.login(deactivatedStaffLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = browser.get("/_test/organizations/" + orgDeactivatedId + "/staff");

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"status\":403");
		assertThat(response.body()).contains("Access is denied");
	}

	@Test
	@DisplayName("Global PLATFORM_ADMIN without membership receives 403 Forbidden (no platform bypass)")
	void platformAdminWithoutMembership_Returns403() throws Exception {
		Browser browser = new Browser();
		browser.login(platformAdminLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = browser.get("/_test/organizations/" + orgAlphaId + "/admin");

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"status\":403");
		assertThat(response.body()).contains("Access is denied");
	}

	@Test
	@DisplayName("Global ORGANIZATION_ADMIN without membership receives 403 Forbidden (global role != tenant auth)")
	void globalOrgAdminWithoutMembership_Returns403() throws Exception {
		Browser browser = new Browser();
		browser.login(orgAdminGlobalLogin, DEFAULT_PASSWORD);

		HttpResponse<String> response = browser.get("/_test/organizations/" + orgAlphaId + "/admin");

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.body()).contains("\"status\":403");
		assertThat(response.body()).contains("Access is denied");
	}

	@Test
	@DisplayName("Revocation mid-session: Member is active, logs in, is revoked in DB; next request with same session receives 403")
	void midSessionRevocation_EnforcedImmediatelyOnSubsequentRequest() throws Exception {
		String login = "revocation-target-" + System.nanoTime() + "@example.com";
		UserAccount user = userAccounts.createUser(login, "Revocation Target", DEFAULT_PASSWORD, Set.of(RoleName.STAFF));
		Organization org = organizationRepository.findById(orgAlphaId).orElseThrow();
		OrganizationMembership membership = membershipRepository.save(new OrganizationMembership(
				org, user, OrgRole.ORG_STAFF, MembershipStatus.ACTIVE));

		Browser browser = new Browser();
		browser.login(login, DEFAULT_PASSWORD);

		// First request succeeds
		HttpResponse<String> firstResponse = browser.get("/_test/organizations/" + orgAlphaId + "/staff");
		assertThat(firstResponse.statusCode()).isEqualTo(200);

		// Revoke membership directly in database
		membership.revoke();
		membershipRepository.save(membership);

		// Subsequent request with the exact same active session cookie is immediately denied
		HttpResponse<String> subsequentResponse = browser.get("/_test/organizations/" + orgAlphaId + "/staff");
		assertThat(subsequentResponse.statusCode()).isEqualTo(403);
		assertThat(subsequentResponse.body()).contains("Access is denied");
	}

	@Test
	@DisplayName("Demotion mid-session: Admin member is demoted to staff in DB; next admin request receives 403")
	void midSessionDemotion_EnforcedImmediatelyOnSubsequentRequest() throws Exception {
		String login = "demotion-target-" + System.nanoTime() + "@example.com";
		UserAccount user = userAccounts.createUser(login, "Demotion Target", DEFAULT_PASSWORD, Set.of(RoleName.STAFF));
		Organization org = organizationRepository.findById(orgAlphaId).orElseThrow();
		OrganizationMembership membership = membershipRepository.save(new OrganizationMembership(
				org, user, OrgRole.ORGANIZATION_ADMIN, MembershipStatus.ACTIVE));

		Browser browser = new Browser();
		browser.login(login, DEFAULT_PASSWORD);

		// Admin request succeeds initially
		HttpResponse<String> firstResponse = browser.get("/_test/organizations/" + orgAlphaId + "/admin");
		assertThat(firstResponse.statusCode()).isEqualTo(200);

		// Demote role to ORG_STAFF in database
		membership.changeRole(OrgRole.ORG_STAFF);
		membershipRepository.save(membership);

		// Subsequent admin request with the exact same active session cookie is immediately denied
		HttpResponse<String> subsequentResponse = browser.get("/_test/organizations/" + orgAlphaId + "/admin");
		assertThat(subsequentResponse.statusCode()).isEqualTo(403);
		assertThat(subsequentResponse.body()).contains("Access is denied");

		// But staff request still succeeds
		HttpResponse<String> staffResponse = browser.get("/_test/organizations/" + orgAlphaId + "/staff");
		assertThat(staffResponse.statusCode()).isEqualTo(200);
	}

	private String credentials(String login, String password) {
		return "{\"loginIdentifier\":\"" + escapeJson(login) + "\",\"password\":\"" + escapeJson(password) + "\"}";
	}

	private String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private String jsonField(String body, String field) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
		assertThat(matcher.find()).as("JSON field %s in %s", field, body).isTrue();
		return matcher.group(1);
	}

	private URI baseUri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	@TestConfiguration
	static class ProbeConfiguration {

		@Bean
		OrganizationTestProbeController organizationTestProbeController() {
			return new OrganizationTestProbeController();
		}

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		SecurityFilterChain organizationProbeSecurityFilterChain(
				HttpSecurity http,
				ApiSecurityErrorHandler errorHandler,
				SecurityContextRepository securityContextRepository) throws Exception {
			http
					.securityMatcher("/_test/organizations/**")
					.csrf(AbstractHttpConfigurer::disable)
					.securityContext(context -> context.securityContextRepository(securityContextRepository))
					.authorizeHttpRequests(requests -> requests
							.anyRequest().authenticated())
					.exceptionHandling(exceptions -> exceptions
							.authenticationEntryPoint(errorHandler)
							.accessDeniedHandler(errorHandler));
			return http.build();
		}
	}

	@RestController
	@RequestMapping("/_test/organizations")
	static class OrganizationTestProbeController {

		@GetMapping("/{organizationId}/staff")
		@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORG_STAFF)")
		public Map<String, Object> staffOperation(@PathVariable("organizationId") Long organizationId) {
			return Map.of("status", "ok", "organizationId", organizationId, "level", "staff");
		}

		@GetMapping("/{organizationId}/admin")
		@PreAuthorize("@orgAuth.canAccessOrg(#organizationId, T(com.rahul.HostelOs.organization.OrgRole).ORGANIZATION_ADMIN)")
		public Map<String, Object> adminOperation(@PathVariable("organizationId") Long organizationId) {
			return Map.of("status", "ok", "organizationId", organizationId, "level", "admin");
		}
	}

	private final class Browser {

		private final CookieManager cookieManager = new CookieManager();
		private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();
		private String csrfHeader;
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
			return postJson("/api/v1/auth/login", credentials(login, password), csrfToken);
		}

		HttpResponse<String> get(String path) throws Exception {
			return send("GET", path, null, null);
		}

		HttpResponse<String> postJson(String path, String body, String token) throws Exception {
			return send("POST", path, body, token);
		}

		private HttpResponse<String> send(String method, String path, String body, String token) throws Exception {
			HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri(path));
			if (body == null) {
				builder.method(method, HttpRequest.BodyPublishers.noBody());
			} else {
				builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.method(method, HttpRequest.BodyPublishers.ofString(body));
			}
			if (token != null) {
				builder.header(csrfHeader, token);
			}
			return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		}
	}
}
