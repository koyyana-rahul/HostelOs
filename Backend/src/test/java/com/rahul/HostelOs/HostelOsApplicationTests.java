package com.rahul.HostelOs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HostelOsApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private List<SecurityFilterChain> securityFilterChains;

	private final HttpClient client = HttpClient.newHttpClient();

	@Test
	void contextLoads() {
	}

	@Test
	void productionSecurityChainIsTheOnlyChain() {
		assertThat(securityFilterChains).hasSize(1);
		assertThat(securityFilterChains.get(0).toString())
				.contains("SecurityConfig")
				.contains("any request");
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		HttpResponse<String> response = get("/actuator/health");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"status\":\"UP\"");
		assertThat(response.body()).doesNotContain("\"components\"");
		assertThat(response.body()).doesNotContain("\"details\"");
	}

	@Test
	void openApiDocumentIsPublic() throws Exception {
		HttpResponse<String> response = get("/v3/api-docs");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body())
				.contains("\"openapi\"")
				.contains("HostelOS API")
				.contains("sessionAuth")
				.contains("/api/v1/auth/login");
	}

	@Test
	void swaggerUiIsPublic() throws Exception {
		HttpResponse<String> response = get("/swagger-ui/index.html");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("Swagger UI");
	}

	@Test
	void systemInfoEndpointIsPublicAndReturnsStableContract() throws Exception {
		HttpResponse<String> response = get("/api/v1/system/info");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith(MediaType.APPLICATION_JSON_VALUE);
		assertThat(response.body())
				.isEqualTo("{\"applicationName\":\"HostelOS\",\"apiVersion\":\"v1\",\"status\":\"AVAILABLE\"}");
	}

	@Test
	void systemInfoEndpointRejectsPost() throws Exception {
		HttpResponse<String> response = postJson("/api/v1/system/info", "{}");

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith(MediaType.APPLICATION_JSON_VALUE);
		assertThat(response.body()).contains("\"status\":401");
		assertThat(response.body()).contains("Authentication is required");
	}

	@Test
	void systemInfoEndpointRejectsHeadWithoutABody() throws Exception {
		HttpResponse<String> response = request("HEAD", "/api/v1/system/info", null);

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.body()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "PUT", "DELETE", "PATCH" })
	void systemInfoEndpointRejectsUnsupportedMethods(String method) throws Exception {
		HttpResponse<String> response = request(method, "/api/v1/system/info", "{}");

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.body()).contains("\"status\":401");
		assertThat(response.body()).contains("Authentication is required");
	}

	@Test
	void unlistedEndpointsAreDenied() throws Exception {
		HttpResponse<String> response = get("/api/not-implemented");

		assertThat(response.statusCode()).isEqualTo(401);
		assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith(MediaType.APPLICATION_JSON_VALUE);
		assertThat(response.body()).contains("\"status\":401");
		assertThat(response.body()).contains("Authentication is required");
	}

	private HttpResponse<String> get(String path) throws Exception {
		return request("GET", path, null);
	}

	private HttpResponse<String> postJson(String path, String body) throws Exception {
		return request("POST", path, body);
	}

	private HttpResponse<String> request(String method, String path, String body) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri(path));
		if (body == null) {
			builder.method(method, HttpRequest.BodyPublishers.noBody());
		} else {
			builder.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.method(method, HttpRequest.BodyPublishers.ofString(body));
		}
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private URI baseUri(String path) {
		return URI.create("http://localhost:" + port + path);
	}
}
