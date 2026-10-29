package com.rahul.HostelOs;

import jakarta.servlet.SessionCookieConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "prod" })
class ProductionDocumentationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private ServletWebServerApplicationContext applicationContext;

	private final HttpClient client = HttpClient.newHttpClient();

	@Test
	void apiDocsAreDisabledInProduction() throws Exception {
		HttpResponse<String> response = get("/v3/api-docs");

		assertThat(response.statusCode()).isEqualTo(404);
	}

	@Test
	void swaggerUiIsDisabledInProduction() throws Exception {
		HttpResponse<String> response = get("/swagger-ui/index.html");

		assertThat(response.statusCode()).isEqualTo(404);
	}

	@Test
	void systemInfoRemainsPublicInProduction() throws Exception {
		HttpResponse<String> response = get("/api/v1/system/info");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.isEqualTo("{\"applicationName\":\"HostelOS\",\"apiVersion\":\"v1\",\"status\":\"AVAILABLE\"}");
	}

	@Test
	void unlistedEndpointsRemainDeniedInProduction() throws Exception {
		HttpResponse<String> response = get("/api/not-implemented");

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.body()).contains("\"status\":401");
		assertThat(response.body()).contains("Authentication is required");
	}

	@Test
	void healthRemainsPublicInProduction() throws Exception {
		HttpResponse<String> response = get("/actuator/health");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"status\":\"UP\"");
	}

	@Test
	void sessionCookieIsHardenedInProduction() {
		SessionCookieConfig cookie = applicationContext.getServletContext().getSessionCookieConfig();

		assertThat(cookie.getName()).isEqualTo("HOSTELOS_SESSION");
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.isSecure()).isTrue();
	}

	@Test
	void csrfCookieIsSecureInProduction() throws Exception {
		HttpResponse<String> response = get("/api/v1/auth/csrf");

		assertThat(response.statusCode()).isEqualTo(200);
		String csrfCookie = response.headers().allValues("set-cookie").stream()
				.filter(value -> value.startsWith("XSRF-TOKEN="))
				.findFirst()
				.orElseThrow();
		assertThat(csrfCookie).contains("Secure");
		assertThat(csrfCookie).contains("SameSite=Lax");
	}

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}
}
