package com.rahul.HostelOs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "prod" })
class ProductionDocumentationTests {

	@LocalServerPort
	private int port;

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

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}
}
