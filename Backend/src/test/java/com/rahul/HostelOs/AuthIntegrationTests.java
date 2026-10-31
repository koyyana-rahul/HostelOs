package com.rahul.HostelOs;

import com.rahul.HostelOs.identity.AccountStatus;
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
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AuthIntegrationTests.AuthTestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthIntegrationTests {

	protected static final String PASSWORD = "correct-horse-battery-staple";

	protected static final String PLATFORM_ADMIN_LOGIN = "platform-admin@example.com";
	protected static final String ORGANIZATION_ADMIN_LOGIN = "organization-admin@example.com";
	protected static final String STAFF_LOGIN = "staff@example.com";
	protected static final String DISABLED_LOGIN = "disabled@example.com";

	@LocalServerPort
	protected int port;

	@Autowired
	protected UserAccountService userAccounts;

	@Autowired
	protected UserAccountRepository userAccountRepository;

	@Autowired
	protected RoleRepository roleRepository;

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

	protected UserAccount createIfMissing(String login, String displayName, RoleName role) {
		return userAccountRepository.findByLoginIdentifier(login)
				.orElseGet(() -> userAccounts.createUser(login, displayName, PASSWORD, Set.of(role)));
	}

	protected String credentials(String login, String password) {
		return "{\"loginIdentifier\":\"" + escapeJson(login) + "\",\"password\":\"" + escapeJson(password) + "\"}";
	}

	protected String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	protected String setCookie(HttpResponse<?> response, String name) {
		return response.headers().allValues("set-cookie").stream()
				.filter(value -> value.startsWith(name + "="))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Missing Set-Cookie for " + name + ": " + response.headers().map()));
	}

	protected String jsonField(String body, String field) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
		org.junit.jupiter.api.Assertions.assertTrue(matcher.find(), "JSON field " + field + " in " + body);
		return matcher.group(1);
	}

	protected URI baseUri(String path) {
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
	@TestComponent
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

	protected class Browser {

		private final CookieManager cookieManager = new CookieManager();
		private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();
		private String csrfHeader;
		private String csrfToken;

		public String getCsrfToken() {
			return csrfToken;
		}

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

		HttpResponse<String> putJson(String path, String body, String token) throws Exception {
			return send("PUT", path, body, token);
		}

		HttpResponse<String> delete(String path, String token) throws Exception {
			return send("DELETE", path, null, token);
		}

		boolean hasSessionCookie() {
			return cookieManager.getCookieStore().getCookies().stream()
					.anyMatch(cookie -> "HOSTELOS_SESSION".equals(cookie.getName()));
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
