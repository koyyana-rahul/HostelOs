package com.rahul.HostelOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ErrorHandlingIntegrationTests.ProbeConfiguration.class)
class ErrorHandlingIntegrationTests {

	@LocalServerPort
	private int port;

	private final HttpClient client = HttpClient.newHttpClient();

	@Test
	void validationFailuresUseTheSharedErrorShape() throws Exception {
		HttpResponse<String> response = postJson("/probe/validate", "{}");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("\"status\":400");
		assertThat(response.body()).contains("\"violations\"");
		assertThat(response.body()).contains("\"field\":\"name\"");
		assertThat(response.body()).contains("must not be blank");
	}

	@Test
	void unexpectedFailuresDoNotLeakInternalDetails() throws Exception {
		HttpResponse<String> response = get("/probe/failure");

		assertThat(response.statusCode()).isEqualTo(500);
		assertThat(response.body()).contains("An unexpected error occurred");
		assertThat(response.body()).doesNotContain("sensitive-internal-detail");
	}

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(baseUri(path)).GET().build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> postJson(String path, String body) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(baseUri(path))
				.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private URI baseUri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	@TestConfiguration
	static class ProbeConfiguration {

		@Bean
		ProbeController probeController() {
			return new ProbeController();
		}

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		SecurityFilterChain probeSecurityFilterChain(HttpSecurity http) throws Exception {
			http
					.securityMatcher("/probe/**")
					.csrf(AbstractHttpConfigurer::disable)
					.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
			return http.build();
		}
	}

	@RestController
	static class ProbeController {

		@PostMapping("/probe/validate")
		Map<String, String> validate(@Valid @RequestBody ProbeRequest request) {
			return Map.of("name", request.name());
		}

		@GetMapping("/probe/failure")
		void failure() {
			throw new IllegalStateException("sensitive-internal-detail");
		}
	}

	record ProbeRequest(@NotBlank(message = "must not be blank") String name) {
	}
}
