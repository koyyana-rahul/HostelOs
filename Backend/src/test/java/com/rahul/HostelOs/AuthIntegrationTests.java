//package com.rahul.HostelOs;
//
//import com.rahul.HostelOs.identity.AccountStatus;
//import com.rahul.HostelOs.identity.DuplicateLoginIdentifierException;
//import com.rahul.HostelOs.identity.Role;
//import com.rahul.HostelOs.identity.RoleName;
//import com.rahul.HostelOs.identity.RoleRepository;
//import com.rahul.HostelOs.identity.UserAccount;
//import com.rahul.HostelOs.identity.UserAccountRepository;
//import com.rahul.HostelOs.identity.UserAccountService;
//import jakarta.servlet.http.HttpServletRequest;
//import java.net.CookieManager;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.List;
//import java.util.Locale;
//import java.util.Set;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.MediaType;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")
//@Import(AuthIntegrationTests.AuthTestConfiguration.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class AuthIntegrationTests {
//
//	private static final String PASSWORD = "correct-horse-battery-staple";
//	private static final String PLATFORM_ADMIN_LOGIN = "platform-admin@example.com";
//	private static final String ORGANIZATION_ADMIN_LOGIN = "organization-admin@example.com";
//	private static final String STAFF_LOGIN = "staff@example.com";
//	private static final String DISABLED_LOGIN = "disabled@example.com";
//
//	// Passwords used for password-length validation tests.
//	// PASSWORD_72BYTES has exactly 72 'A's = 72 UTF-8 bytes (boundary case).
//	// PASSWORD_80BYTES has 80 'A's = 80 UTF-8 bytes (oversized case).
//	// PASSWORD_MULTIBYTE has 30 euro signs (U+20AC) = 90 UTF-8 bytes (> 72).
//private static final String PASSWORD_72BYTES =
//		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // 72 'A's = 72 UTF-8 bytes
//private static final String PASSWORD_80BYTES =
//		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // 80 'A's = 80 UTF-8 bytes
//	private static final String PASSWORD_MULTIBYTE =
//		"\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC\u20AC"; // 30 euro signs = 90 UTF-8 bytes, <200 Java chars
//
//	@LocalServerPort
//	private int port;
//
//	private Browser browser;
//
//	@Autowired
//	private UserAccountService userAccounts;
//
//	@Autowired
//	private UserAccountRepository userAccountRepository;
//
//	@Autowired
//	private RoleRepository roleRepository;
//
//	@BeforeAll
//	void createIdentityFixtures() {
//		createIfMissing(PLATFORM_ADMIN_LOGIN, "Platform Administrator", RoleName.PLATFORM_ADMIN);
//		createIfMissing(ORGANIZATION_ADMIN_LOGIN, "Organization Administrator", RoleName.ORGANIZATION_ADMIN);
//		createIfMissing(STAFF_LOGIN, "Staff Member", RoleName.STAFF);
//		UserAccount disabled = createIfMissing(DISABLED_LOGIN, "Disabled User", RoleName.STAFF);
//		if (disabled.getStatus() != AccountStatus.DISABLED) {
//			disabled.disable();
//userAccountRepository.save(disabled);
//    browser = new Browser();
//	}
//	}
//
//	@Test
//	void csrfBootstrapReturnsTokenAndCookie() throws Exception {
//		Browser browser = new Browser();
//
//		HttpResponse<String> response = browser.bootstrapCsrf();
//
//		assertThat(response.statusCode()).isEqualTo(200);
//		assertThat(response.body()).contains("\"headerName\":\"X-XSRF-TOKEN\"");
//		assertThat(response.body()).contains("\"token\":\"");
//		String csrfCookie = setCookie(response, "XSRF-TOKEN");
//		assertThat(csrfCookie).doesNotContain("HttpOnly");
//		assertThat(csrfCookie).contains("Path=/");
//		assertThat(csrfCookie).contains("SameSite=Lax");
//		assertThat(csrfCookie).doesNotContain("Secure");
//	}
//
//	@Test
//	void loginWithoutCsrfIsRejected() throws Exception {
//		Browser browser = new Browser();
//
//		HttpResponse<String> response = browser.postJson(
//				"/api/v1/auth/login",
//				credentials(PLATFORM_ADMIN_LOGIN, PASSWORD),
//				null);
//
//		assertThat(response.statusCode()).isEqualTo(403);
//		assertThat(browser.hasSessionCookie()).isFalse();
//	}
//
//	@Test
//	void loginCreatesRotatedHttpOnlySessionAndMeReturnsSafeIdentity() throws Exception {
//		Browser browser = new Browser();
//		browser.bootstrapCsrf();
//
//		HttpResponse<String> login = browser.login(
//				"  " + PLATFORM_ADMIN_LOGIN.toUpperCase(Locale.ROOT) + "  ",
//				PASSWORD);
//
//		assertThat(login.statusCode()).isEqualTo(200);
//		assertThat(login.body())
//				.contains("\"loginIdentifier\":\"" + PLATFORM_ADMIN_LOGIN + "\"")
//				.contains("\"displayName\":\"Platform Administrator\"")
//				.contains("\"roles\":[\"PLATFORM_ADMIN\"]")
//				.doesNotContain("password")
//				.doesNotContain("$2");
//		String sessionCookie = setCookie(login, "HOSTELOS_SESSION");
//		assertThat(sessionCookie).contains("HttpOnly");
//		assertThat(sessionCookie).contains("SameSite=Lax");
//		assertThat(sessionCookie).contains("Path=/");
//		assertThat(sessionCookie).doesNotContain("Secure");
//		assertThat(browser.hasSessionCookie()).isTrue();
//
//		HttpResponse<String> me = browser.get("/api/v1/auth/me");
//		assertThat(me.statusCode()).isEqualTo(200);
//		assertThat(me.body())
//				.contains("\"loginIdentifier\":\"" + PLATFORM_ADMIN_LOGIN + "\"")
//				.contains("\"roles\":[\"PLATFORM_ADMIN\"]")
//				.doesNotContain("password")
//				.doesNotContain("$2");
//	}
//
//	@Test
//	void loginRotatesAnExistingSessionId() throws Exception {
//		Browser browser = new Browser();
//		String previousSessionId = browser.createSession();
//		browser.bootstrapCsrf();
//
//		HttpResponse<String> login = browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD);
//
//		assertThat(login.statusCode()).isEqualTo(200);
//		String sessionCookie = setCookie(login, "HOSTELOS_SESSION");
//		assertThat(sessionCookie).doesNotContain("HOSTELOS_SESSION=" + previousSessionId + ";");
//	}
//
//	@Test
//	void meWithoutSessionIsUnauthorized() throws Exception {
//		HttpResponse<String> response = new Browser().get("/api/v1/auth/me");
//
//		assertThat(response.statusCode()).isEqualTo(401);
//		assertThat(response.body()).contains("Authentication is required");
//	}
//
//	@Test
//	void invalidPasswordAndUnknownLoginReturnTheSameGenericFailure() throws Exception {
//		Browser wrongPassword = new Browser();
//		wrongPassword.bootstrapCsrf();
//		HttpResponse<String> wrongPasswordResponse = wrongPassword.login(PLATFORM_ADMIN_LOGIN, "wrong-password");
//
//		Browser unknownLogin = new Browser();
//		unknownLogin.bootstrapCsrf();
//		String unknownIdentifier = "unknown-" + System.nanoTime() + "@example.com";
//		HttpResponse<String> unknownLoginResponse = unknownLogin.login(unknownIdentifier, PASSWORD);
//
//		assertThat(wrongPasswordResponse.statusCode()).isEqualTo(401);
//		assertThat(unknownLoginResponse.statusCode()).isEqualTo(401);
//		for (HttpResponse<String> response : List.of(wrongPasswordResponse, unknownLoginResponse)) {
//			assertThat(response.body())
//					.contains("\"status\":401")
//					.contains("Invalid credentials")
//					.doesNotContain(PLATFORM_ADMIN_LOGIN)
//					.doesNotContain(unknownIdentifier)
//					.doesNotContain("disabled");
//		}
//		assertThat(wrongPassword.hasSessionCookie()).isFalse();
//		assertThat(unknownLogin.hasSessionCookie()).isFalse();
//	}
//
//	@Test
//	void disabledAccountCannotLogin() throws Exception {
//		Browser browser = new Browser();
//		browser.bootstrapCsrf();
//
//		HttpResponse<String> response = browser.login(DISABLED_LOGIN, PASSWORD);
//
//		assertThat(response.statusCode()).isEqualTo(401);
//		assertThat(response.body()).contains("Invalid credentials");
//		assertThat(browser.hasSessionCookie()).isFalse();
//	}
//
//	@Test
//	void disablingAnAccountInvalidatesItsExistingSession() throws Exception {
//		String login = "session-revoke-" + System.nanoTime() + "@example.com";
//		UserAccount account = userAccounts.createUser(login, "Session Revoke", PASSWORD, Set.of(RoleName.STAFF));
//		Browser browser = new Browser();
//		browser.bootstrapCsrf();
//		assertThat(browser.login(login, PASSWORD).statusCode()).isEqualTo(200);
//		assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(200);
//
//		account.disable();
//		userAccountRepository.save(account);
//
//		HttpResponse<String> me = browser.get("/api/v1/auth/me");
//		assertThat(me.statusCode()).isEqualTo(401);
//		assertThat(me.body()).contains("Authentication is required");
//	}
//
//	@Test
//	void authenticatedStateChangingRequestsRequireCsrf() throws Exception {
//		Browser browser = new Browser();
//		browser.bootstrapCsrf();
//		assertThat(browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD).statusCode()).isEqualTo(200);
//
//		HttpResponse<String> withoutToken = browser.postJson("/api/v1/auth/logout", "", null);
//		assertThat(withoutToken.statusCode()).isEqualTo(403);
//		assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(200);
//
//		HttpResponse<String> withToken = browser.postJson("/api/v1/auth/logout", "", browser.csrfToken);
//		assertThat(withToken.statusCode()).isEqualTo(204);
//		assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
//	}
//
//	@Test
//	void repeatedFailuresAreThrottled() throws Exception {
//		String login = "rate-limit-" + System.nanoTime() + "@example.com";
//		Browser browser = new Browser();
//		browser.bootstrapCsrf();
//
//		for (int attempt = 0; attempt < 5; attempt++) {
//			assertThat(browser.login(login, "wrong-password").statusCode()).isEqualTo(401);
//		}
//
//		HttpResponse<String> throttled = browser.login(login, "wrong-password");
//		assertThat(throttled.statusCode()).isEqualTo(429);
//		assertThat(throttled.body()).contains("Too many authentication attempts");
//	}
//
//	@Test
//	void normalizedLoginIdentifiersCannotBeDuplicated() throws Exception {
//		String login = "duplicate-" + System.nanoTime() + "@example.com";
//		userAccounts.createUser("  " + login.toUpperCase(Locale.ROOT) + "  ", "Duplicate One", PASSWORD, Set.of(RoleName.STAFF));
//
//		assertThatThrownBy(() -> userAccounts.createUser(login, "Duplicate Two", PASSWORD, Set.of(RoleName.STAFF)))
//				.isInstanceOf(DuplicateLoginIdentifierException.class);
//
//		UserAccount stored = userAccountRepository.findByLoginIdentifier(login).orElseThrow();
//		assertThat(stored.getLoginIdentifier()).isEqualTo(login);
//		assertThat(stored.getPasswordHash())
//				.startsWith("$2")
//				.hasSize(60)
//				.doesNotContain(PASSWORD);
//	}
//
//	@Test
//	void bootstrapAdminCreationIsIdempotent() throws Exception {
//		String login = "bootstrap-" + System.nanoTime() + "@example.com";
//
//		assertThat(userAccounts.createPlatformAdminIfAbsent(login, "Bootstrap Admin", PASSWORD)).isTrue();
//		assertThat(userAccounts.createPlatformAdminIfAbsent(login, "Ignored", PASSWORD)).isFalse();
//
//		UserAccount created = userAccountRepository.findByLoginIdentifier(login).orElseThrow();
//		assertThat(created.getRoles()).extracting(Role::getName).containsExactly(RoleName.PLATFORM_ADMIN);
//		assertThat(created.getPasswordHash()).startsWith("$2").hasSize(60).doesNotContain(PASSWORD);
//	}
//
//	@Test
//	void roleAuthorizationIsEnforcedOnTheServer() throws Exception {
//		Browser platformAdmin = new Browser();
//		platformAdmin.bootstrapCsrf();
//		assertThat(platformAdmin.login(PLATFORM_ADMIN_LOGIN, PASSWORD).statusCode()).isEqualTo(200);
//		assertThat(platformAdmin.get("/_test/roles/platform").statusCode()).isEqualTo(200);
//		assertThat(platformAdmin.get("/_test/roles/organization").statusCode()).isEqualTo(403);
//		assertThat(platformAdmin.get("/_test/roles/staff").statusCode()).isEqualTo(403);
//
//		Browser organizationAdmin = new Browser();
//		organizationAdmin.bootstrapCsrf();
//		assertThat(organizationAdmin.login(ORGANIZATION_ADMIN_LOGIN, PASSWORD).statusCode()).isEqualTo(200);
//		assertThat(organizationAdmin.get("/_test/roles/platform").statusCode()).isEqualTo(403);
//		assertThat(organizationAdmin.get("/_test/roles/organization").statusCode()).isEqualTo(200);
//		assertThat(organizationAdmin.get("/_test/roles/staff").statusCode()).isEqualTo(403);
//
//		Browser staff = new Browser();
//		staff.bootstrapCsrf();
//		assertThat(staff.login(STAFF_LOGIN, PASSWORD).statusCode()).isEqualTo(200);
//		assertThat(staff.get("/_test/roles/platform").statusCode()).isEqualTo(403);
//		assertThat(staff.get("/_test/roles/organization").statusCode()).isEqualTo(403);
//		assertThat(staff.get("/_test/roles/staff").statusCode()).isEqualTo(200);
//	}
//
//	@Test
//	void rolesAreSeededWithDistinctPlatformAndOrganizationAdministration() {
//		assertThat(roleRepository.findAll())
//				.extracting(Role::getName)
//				.containsExactlyInAnyOrder(
//						RoleName.PLATFORM_ADMIN,
//						RoleName.ORGANIZATION_ADMIN,
//						RoleName.STAFF);
//	}
//
//	@Test
//	void loginRequestCannotChooseRoles() throws Exception {
//		Browser browser = new Browser();
//		browser.bootstrapCsrf();
//		String payload = "{\"loginIdentifier\":\"" + STAFF_LOGIN
//				+ "\",\"password\":\"" + PASSWORD
//				+ "\",\"roles\":[\"PLATFORM_ADMIN\"]}";
//
//		HttpResponse<String> login = browser.postJson("/api/v1/auth/login", payload, browser.csrfToken);
//
//		assertThat(login.statusCode()).isEqualTo(200);
//		assertThat(login.body())
//				.contains("\"roles\":[\"STAFF\"]")
//				.doesNotContain("PLATFORM_ADMIN");
//		assertThat(browser.get("/_test/roles/platform").statusCode()).isEqualTo(403);
//		assertThat(browser.get("/_test/roles/staff").statusCode()).isEqualTo(200);
//	}
//
//	private UserAccount createIfMissing(String login, String displayName, RoleName role) {
//		return userAccountRepository.findByLoginIdentifier(login)
//				.orElseGet(() -> userAccounts.createUser(login, displayName, PASSWORD, Set.of(role)));
//	}
//
//	private String credentials(String login, String password) {
//		return "{\"loginIdentifier\":\"" + escapeJson(login) + "\",\"password\":\"" + escapeJson(password) + "\"}";
//	}
//
//	private String escapeJson(String value) {
//		return value.replace("\\", "\\\\").replace("\"", "\\\"");
//	}
//
//	private String setCookie(HttpResponse<?> response, String name) {
//		return response.headers().allValues("set-cookie").stream()
//				.filter(value -> value.startsWith(name + "="))
//				.findFirst()
//				.orElseThrow(() -> new AssertionError("Missing Set-Cookie for " + name + ": " + response.headers().map()));
//	}
//
//	private String jsonField(String body, String field) {
//		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
//		assertThat(matcher.find()).as("JSON field %s in %s", field, body).isTrue();
//		return matcher.group(1);
//	}
//
//	private URI baseUri(String path) {
//		return URI.create("http://localhost:" + port + path);
//	}
//
//	@TestConfiguration
//	static class AuthTestConfiguration {
//
//		@Bean
//		AuthTestController authTestController() {
//			return new AuthTestController();
//		}
//
//		@Bean
//		@Order(Ordered.HIGHEST_PRECEDENCE)
//		SecurityFilterChain authProbeSecurityFilterChain(HttpSecurity http) throws Exception {
//			http
//					.securityMatcher("/_test/**")
//					.csrf(AbstractHttpConfigurer::disable)
//					.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
//			return http.build();
//		}
//	}
//
//	@RestController
//	static class AuthTestController {
//
//		@GetMapping("/_test/roles/platform")
//		@PreAuthorize("hasRole('PLATFORM_ADMIN')")
//		String platformAdminOnly() {
//			return "platform";
//		}
//
//		@GetMapping("/_test/roles/organization")
//		@PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
//		String organizationAdminOnly() {
//			return "organization";
//		}
//
//		@GetMapping("/_test/roles/staff")
//		@PreAuthorize("hasRole('STAFF')")
//		String staffOnly() {
//			return "staff";
//		}
//
//		@GetMapping("/_test/session")
//		String createSession(HttpServletRequest request) {
//			return request.getSession(true).getId();
//		}
//	}
//
//	private final class Browser {
//
//		private final CookieManager cookieManager = new CookieManager();
//		private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();
//		private String csrfHeader;
//		private String csrfToken;
//
//		public void clearCookies() {
//			cookieManager.getCookieStore().getCookies().clear();
//		}
//
//		HttpResponse<String> bootstrapCsrf() throws Exception {
//			HttpResponse<String> response = get("/api/v1/auth/csrf");
//			csrfHeader = jsonField(response.body(), "headerName");
//			csrfToken = jsonField(response.body(), "token");
//			return response;
//		}
//
//		HttpResponse<String> login(String login, String password) throws Exception {
//			if (csrfToken == null) {
//				bootstrapCsrf();
//			}
//			return postJson("/api/v1/auth/login", credentials(login, password), csrfToken);
//		}
//
//		String createSession() throws Exception {
//			return get("/_test/session").body();
//		}
//
//		HttpResponse<String> get(String path) throws Exception {
//			return send("GET", path, null, null);
//		}
//
//		HttpResponse<String> postJson(String path, String body, String token) throws Exception {
//			return send("POST", path, body, token);
//		}
//
//		boolean hasSessionCookie() {
//			return cookieManager.getCookieStore().getCookies().stream()
//					.anyMatch(cookie -> "HOSTELOS_SESSION".equals(cookie.getName()));
//		}
//
//		boolean hasSessionCookiePublic() {
//			return hasSessionCookie();
//		}
//
//		private HttpResponse<String> send(String method, String path, String body, String token) throws Exception {
//			HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri(path));
//			if (body == null) {
//				builder.method(method, HttpRequest.BodyPublishers.noBody());
//			} else {
//				builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//						.method(method, HttpRequest.BodyPublishers.ofString(body));
//			}
//			if (token != null) {
//				builder.header(csrfHeader, token);
//			}
//			return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
//}
//	}
//	@Test
//	void loginWithValidPasswordCreatesSession() throws Exception {
//		browser.bootstrapCsrf();
//		HttpResponse<String> response = browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD);
//		assertThat(response.statusCode()).isEqualTo(200);
//		assertThat(browser.hasSessionCookiePublic()).isTrue();
//	}
//
//	@Test
//	void loginWithPasswordExactly72BytesDoesNotReturn400() throws Exception {
//		browser.bootstrapCsrf();
//		HttpResponse<String> response = browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD_72BYTES);
//		// The byte‑length check must not reject it; authentication may still fail,
//		// but the response should not be a validation 400.
//		assertThat(response.statusCode()).isNotEqualTo(400);
//	}
//
//	@Test
//	void loginWithPasswordExceeds72BytesReturns400() throws Exception {
//		browser.bootstrapCsrf();
//		HttpResponse<String> response = browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD_80BYTES);
//		assertThat(response.statusCode()).isEqualTo(400);
//	}
//
//	@Test
//	void loginWithMultibytePasswordExceeds72BytesReturns400() throws Exception {
//		browser.bootstrapCsrf();
//		HttpResponse<String> response = browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD_MULTIBYTE);
//		assertThat(response.statusCode()).isEqualTo(400);
//	}
//}



package com.rahul.HostelOs;

import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.DuplicateLoginIdentifierException;
import com.rahul.HostelOs.identity.Role;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.RoleRepository;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.identity.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AuthIntegrationTests.AuthTestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthIntegrationTests {

    private static final String PASSWORD = "correct-horse-battery-staple";
    private static final String PLATFORM_ADMIN_LOGIN = "platform-admin@example.com";
    private static final String ORGANIZATION_ADMIN_LOGIN = "organization-admin@example.com";
    private static final String STAFF_LOGIN = "staff@example.com";
    private static final String DISABLED_LOGIN = "disabled@example.com";

    // Passwords used for password-length validation tests.
    // ASCII means character count equals UTF-8 byte count for these two cases.
    private static final String PASSWORD_72BYTES = "A".repeat(72);
    private static final String PASSWORD_80BYTES = "A".repeat(80);
    // 30 euro signs (U+20AC) = 90 UTF-8 bytes, while only 30 Unicode characters.
    private static final String PASSWORD_MULTIBYTE = "\\u20AC".repeat(30);

    @LocalServerPort
    private int port;

    @Autowired
    private UserAccountService userAccounts;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeAll
    void createIdentityFixtures() {
        createIfMissing(PLATFORM_ADMIN_LOGIN, "Platform Administrator", RoleName.PLATFORM_ADMIN);
        createIfMissing(ORGANIZATION_ADMIN_LOGIN, "Organization Administrator", RoleName.ORGANIZATION_ADMIN);
        createIfMissing(STAFF_LOGIN, "Staff Member", RoleName.STAFF);
        UserAccount disabled = createIfMissing(DISABLED_LOGIN, "Disabled User", RoleName.STAFF);
        if (disabled.getStatus() != AccountStatus.DISABLED) {
            disabled.disable();
            userAccountRepository.save(disabled);
        }
    }

    @Test
    void csrfBootstrapReturnsTokenAndCookie() throws Exception {
        Browser browser = new Browser();

        HttpResponse<String> response = browser.bootstrapCsrf();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"headerName\":\"X-XSRF-TOKEN\"");
        assertThat(response.body()).contains("\"token\":\"");
        String csrfCookie = setCookie(response, "XSRF-TOKEN");
        assertThat(csrfCookie).doesNotContain("HttpOnly");
        assertThat(csrfCookie).contains("Path=/");
        assertThat(csrfCookie).contains("SameSite=Lax");
        assertThat(csrfCookie).doesNotContain("Secure");
    }

    @Test
    void loginWithoutCsrfIsRejected() throws Exception {
        Browser browser = new Browser();

        HttpResponse<String> response = browser.postJson(
                "/api/v1/auth/login",
                credentials(PLATFORM_ADMIN_LOGIN, PASSWORD),
                null);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(browser.hasSessionCookie()).isFalse();
    }

    @Test
    void loginCreatesRotatedHttpOnlySessionAndMeReturnsSafeIdentity() throws Exception {
        Browser browser = new Browser();
        browser.bootstrapCsrf();

        HttpResponse<String> login = browser.login(
                "  " + PLATFORM_ADMIN_LOGIN.toUpperCase(Locale.ROOT) + "  ",
                PASSWORD);

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body())
                .contains("\"loginIdentifier\":\"" + PLATFORM_ADMIN_LOGIN + "\"")
                .contains("\"displayName\":\"Platform Administrator\"")
                .contains("\"roles\":[\"PLATFORM_ADMIN\"]")
                .doesNotContain("password")
                .doesNotContain("$2");
        String sessionCookie = setCookie(login, "HOSTELOS_SESSION");
        assertThat(sessionCookie).contains("HttpOnly");
        assertThat(sessionCookie).contains("SameSite=Lax");
        assertThat(sessionCookie).contains("Path=/");
        assertThat(sessionCookie).doesNotContain("Secure");
        assertThat(browser.hasSessionCookie()).isTrue();

        HttpResponse<String> me = browser.get("/api/v1/auth/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body())
                .contains("\"loginIdentifier\":\"" + PLATFORM_ADMIN_LOGIN + "\"")
                .contains("\"roles\":[\"PLATFORM_ADMIN\"]")
                .doesNotContain("password")
                .doesNotContain("$2");
    }

    @Test
    void loginRotatesAnExistingSessionId() throws Exception {
        Browser browser = new Browser();
        String previousSessionId = browser.createSession();
        browser.bootstrapCsrf();

        HttpResponse<String> login = browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD);

        assertThat(login.statusCode()).isEqualTo(200);
        String sessionCookie = setCookie(login, "HOSTELOS_SESSION");
        assertThat(sessionCookie).doesNotContain("HOSTELOS_SESSION=" + previousSessionId + ";");
    }

    @Test
    void meWithoutSessionIsUnauthorized() throws Exception {
        HttpResponse<String> response = new Browser().get("/api/v1/auth/me");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("Authentication is required");
    }

    @Test
    void invalidPasswordAndUnknownLoginReturnTheSameGenericFailure() throws Exception {
        Browser wrongPassword = new Browser();
        wrongPassword.bootstrapCsrf();
        HttpResponse<String> wrongPasswordResponse = wrongPassword.login(PLATFORM_ADMIN_LOGIN, "wrong-password");

        Browser unknownLogin = new Browser();
        unknownLogin.bootstrapCsrf();
        String unknownIdentifier = "unknown-" + System.nanoTime() + "@example.com";
        HttpResponse<String> unknownLoginResponse = unknownLogin.login(unknownIdentifier, PASSWORD);

        assertThat(wrongPasswordResponse.statusCode()).isEqualTo(401);
        assertThat(unknownLoginResponse.statusCode()).isEqualTo(401);
        for (HttpResponse<String> response : List.of(wrongPasswordResponse, unknownLoginResponse)) {
            assertThat(response.body())
                    .contains("\"status\":401")
                    .contains("Invalid credentials")
                    .doesNotContain(PLATFORM_ADMIN_LOGIN)
                    .doesNotContain(unknownIdentifier)
                    .doesNotContain("disabled");
        }
        assertThat(wrongPassword.hasSessionCookie()).isFalse();
        assertThat(unknownLogin.hasSessionCookie()).isFalse();
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        Browser browser = new Browser();
        browser.bootstrapCsrf();

        HttpResponse<String> response = browser.login(DISABLED_LOGIN, PASSWORD);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("Invalid credentials");
        assertThat(browser.hasSessionCookie()).isFalse();
    }

    @Test
    void disablingAnAccountInvalidatesItsExistingSession() throws Exception {
        String login = "session-revoke-" + System.nanoTime() + "@example.com";
        UserAccount account = userAccounts.createUser(login, "Session Revoke", PASSWORD, Set.of(RoleName.STAFF));
        Browser browser = new Browser();
        browser.bootstrapCsrf();
        assertThat(browser.login(login, PASSWORD).statusCode()).isEqualTo(200);
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(200);

        account.disable();
        userAccountRepository.save(account);

        HttpResponse<String> me = browser.get("/api/v1/auth/me");
        assertThat(me.statusCode()).isEqualTo(401);
        assertThat(me.body()).contains("Authentication is required");
    }

    @Test
    void authenticatedStateChangingRequestsRequireCsrf() throws Exception {
        Browser browser = new Browser();
        browser.bootstrapCsrf();
        assertThat(browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD).statusCode()).isEqualTo(200);

        HttpResponse<String> withoutToken = browser.postJson("/api/v1/auth/logout", "", null);
        assertThat(withoutToken.statusCode()).isEqualTo(403);
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(200);

        HttpResponse<String> withToken = browser.postJson("/api/v1/auth/logout", "", browser.csrfToken);
        assertThat(withToken.statusCode()).isEqualTo(204);
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
    }

    @Test
    void repeatedFailuresAreThrottled() throws Exception {
        String login = "rate-limit-" + System.nanoTime() + "@example.com";
        Browser browser = new Browser();
        browser.bootstrapCsrf();

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(browser.login(login, "wrong-password").statusCode()).isEqualTo(401);
        }

        HttpResponse<String> throttled = browser.login(login, "wrong-password");
        assertThat(throttled.statusCode()).isEqualTo(429);
        assertThat(throttled.body()).contains("Too many authentication attempts");
    }

    @Test
    void normalizedLoginIdentifiersCannotBeDuplicated() throws Exception {
        String login = "duplicate-" + System.nanoTime() + "@example.com";
        userAccounts.createUser("  " + login.toUpperCase(Locale.ROOT) + "  ", "Duplicate One", PASSWORD, Set.of(RoleName.STAFF));

        assertThatThrownBy(() -> userAccounts.createUser(login, "Duplicate Two", PASSWORD, Set.of(RoleName.STAFF)))
                .isInstanceOf(DuplicateLoginIdentifierException.class);

        UserAccount stored = userAccountRepository.findByLoginIdentifier(login).orElseThrow();
        assertThat(stored.getLoginIdentifier()).isEqualTo(login);
        assertThat(stored.getPasswordHash())
                .startsWith("$2")
                .hasSize(60)
                .doesNotContain(PASSWORD);
    }

    @Test
    void bootstrapAdminCreationIsIdempotent() throws Exception {
        String login = "bootstrap-" + System.nanoTime() + "@example.com";

        assertThat(userAccounts.createPlatformAdminIfAbsent(login, "Bootstrap Admin", PASSWORD)).isTrue();
        assertThat(userAccounts.createPlatformAdminIfAbsent(login, "Ignored", PASSWORD)).isFalse();

        UserAccount created = userAccountRepository.findByLoginIdentifier(login).orElseThrow();
        assertThat(created.getRoles()).extracting(Role::getName).containsExactly(RoleName.PLATFORM_ADMIN);
        assertThat(created.getPasswordHash()).startsWith("$2").hasSize(60).doesNotContain(PASSWORD);
    }

    @Test
    void roleAuthorizationIsEnforcedOnTheServer() throws Exception {
        Browser platformAdmin = new Browser();
        platformAdmin.bootstrapCsrf();
        assertThat(platformAdmin.login(PLATFORM_ADMIN_LOGIN, PASSWORD).statusCode()).isEqualTo(200);
        assertThat(platformAdmin.get("/_test/roles/platform").statusCode()).isEqualTo(200);
        assertThat(platformAdmin.get("/_test/roles/organization").statusCode()).isEqualTo(403);
        assertThat(platformAdmin.get("/_test/roles/staff").statusCode()).isEqualTo(403);

        Browser organizationAdmin = new Browser();
        organizationAdmin.bootstrapCsrf();
        assertThat(organizationAdmin.login(ORGANIZATION_ADMIN_LOGIN, PASSWORD).statusCode()).isEqualTo(200);
        assertThat(organizationAdmin.get("/_test/roles/platform").statusCode()).isEqualTo(403);
        assertThat(organizationAdmin.get("/_test/roles/organization").statusCode()).isEqualTo(200);
        assertThat(organizationAdmin.get("/_test/roles/staff").statusCode()).isEqualTo(403);

        Browser staff = new Browser();
        staff.bootstrapCsrf();
        assertThat(staff.login(STAFF_LOGIN, PASSWORD).statusCode()).isEqualTo(200);
        assertThat(staff.get("/_test/roles/platform").statusCode()).isEqualTo(403);
        assertThat(staff.get("/_test/roles/organization").statusCode()).isEqualTo(403);
        assertThat(staff.get("/_test/roles/staff").statusCode()).isEqualTo(200);
    }

    @Test
    void rolesAreSeededWithDistinctPlatformAndOrganizationAdministration() {
        assertThat(roleRepository.findAll())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder(
                        RoleName.PLATFORM_ADMIN,
                        RoleName.ORGANIZATION_ADMIN,
                        RoleName.STAFF);
    }

    @Test
    void loginRequestCannotChooseRoles() throws Exception {
        Browser browser = new Browser();
        browser.bootstrapCsrf();
        String payload = "{\"loginIdentifier\":\"" + STAFF_LOGIN
                + "\",\"password\":\"" + PASSWORD
                + "\",\"roles\":[\"PLATFORM_ADMIN\"]}";

        HttpResponse<String> login = browser.postJson("/api/v1/auth/login", payload, browser.csrfToken);

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body())
                .contains("\"roles\":[\"STAFF\"]")
                .doesNotContain("PLATFORM_ADMIN");
        assertThat(browser.get("/_test/roles/platform").statusCode()).isEqualTo(403);
        assertThat(browser.get("/_test/roles/staff").statusCode()).isEqualTo(200);
    }

    private UserAccount createIfMissing(String login, String displayName, RoleName role) {
        return userAccountRepository.findByLoginIdentifier(login)
                .orElseGet(() -> userAccounts.createUser(login, displayName, PASSWORD, Set.of(role)));
    }

    private String credentials(String login, String password) {
        return "{\"loginIdentifier\":\"" + escapeJson(login) + "\",\"password\":\"" + escapeJson(password) + "\"}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String setCookie(HttpResponse<?> response, String name) {
        return response.headers().allValues("set-cookie").stream()
                .filter(value -> value.startsWith(name + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Set-Cookie for " + name + ": " + response.headers().map()));
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
    static class AuthTestConfiguration {

        @Bean
        AuthTestController authTestController() {
            return new AuthTestController();
        }

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        SecurityFilterChain authProbeSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .securityMatcher("/_test/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
            return http.build();
        }
    }

    @RestController
    static class AuthTestController {

        @GetMapping("/_test/roles/platform")
        @PreAuthorize("hasRole('PLATFORM_ADMIN')")
        String platformAdminOnly() {
            return "platform";
        }

        @GetMapping("/_test/roles/organization")
        @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
        String organizationAdminOnly() {
            return "organization";
        }

        @GetMapping("/_test/roles/staff")
        @PreAuthorize("hasRole('STAFF')")
        String staffOnly() {
            return "staff";
        }

        @GetMapping("/_test/session")
        String createSession(HttpServletRequest request) {
            return request.getSession(true).getId();
        }
    }

    private final class Browser {

        private final CookieManager cookieManager = new CookieManager();
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();
        private String csrfHeader;
        private String csrfToken;

        public void clearCookies() {
            cookieManager.getCookieStore().getCookies().clear();
        }

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

        String createSession() throws Exception {
            return get("/_test/session").body();
        }

        HttpResponse<String> get(String path) throws Exception {
            return send("GET", path, null, null);
        }

        HttpResponse<String> postJson(String path, String body, String token) throws Exception {
            return send("POST", path, body, token);
        }

        boolean hasSessionCookie() {
            return cookieManager.getCookieStore().getCookies().stream()
                    .anyMatch(cookie -> "HOSTELOS_SESSION".equals(cookie.getName()));
        }

        boolean hasSessionCookiePublic() {
            return hasSessionCookie();
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
    @Test
    void loginWithValidPasswordCreatesSession() throws Exception {
        Browser browser = new Browser();
        browser.bootstrapCsrf();

        HttpResponse<String> response = browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(browser.hasSessionCookie()).isTrue();
    }

    @Test
    void loginWithPasswordExactly72BytesSucceeds() throws Exception {
        String login = "password-72-byte-" + System.nanoTime() + "@example.com";
        userAccounts.createUser(
                login,
                "72 Byte Password User",
                PASSWORD_72BYTES,
                Set.of(RoleName.STAFF));

        Browser browser = new Browser();
        browser.bootstrapCsrf();

        HttpResponse<String> response = browser.login(login, PASSWORD_72BYTES);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"loginIdentifier\":\"" + login + "\"")
                .contains("\"roles\":[\"STAFF\"]");
    }

    @Test
    void loginWithPasswordExceeds72BytesReturns400() throws Exception {
        Browser browser = new Browser();
        browser.bootstrapCsrf();

        HttpResponse<String> response = browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD_80BYTES);

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void loginWithMultibytePasswordExceeds72BytesReturns400() throws Exception {
        Browser browser = new Browser();
        browser.bootstrapCsrf();

        HttpResponse<String> response = browser.login(PLATFORM_ADMIN_LOGIN, PASSWORD_MULTIBYTE);

        assertThat(response.statusCode()).isEqualTo(400);
    }
}
