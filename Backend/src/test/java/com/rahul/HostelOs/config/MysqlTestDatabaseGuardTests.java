package com.rahul.HostelOs.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MysqlTestDatabaseGuardTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(new MysqlTestDatabaseGuard());

    // 1. Valid URL for localhost:3306/hostelos_test
    @Test
    @DisplayName("Valid URL with localhost:3306/hostelos_test passes validation")
    void validLocalhostUrl_Passes() {
        assertThatCode(() -> MysqlTestDatabaseGuard.validate(
            "jdbc:mysql://localhost:3306/hostelos_test",
            "test_user",
            "test_pass"
        )).doesNotThrowAnyException();

        // With optional query parameters
        assertThatCode(() -> MysqlTestDatabaseGuard.validate(
            "jdbc:mysql://localhost:3306/hostelos_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "test_user",
            "test_pass"
        )).doesNotThrowAnyException();
    }

    // 2. Valid URL for 127.0.0.1:3306/hostelos_test
    @Test
    @DisplayName("Valid URL with 127.0.0.1:3306/hostelos_test passes validation")
    void validLoopbackIpUrl_Passes() {
        assertThatCode(() -> MysqlTestDatabaseGuard.validate(
            "jdbc:mysql://127.0.0.1:3306/hostelos_test",
            "test_user",
            "test_pass"
        )).doesNotThrowAnyException();

        // With optional query parameters
        assertThatCode(() -> MysqlTestDatabaseGuard.validate(
            "jdbc:mysql://127.0.0.1:3306/hostelos_test?createDatabaseIfNotExist=false",
            "test_user",
            "test_pass"
        )).doesNotThrowAnyException();
    }

    // 3. Rejection of standard production/dev database name 'hostelos'
    @Test
    @DisplayName("Rejection of non-test database name 'hostelos'")
    void defaultHostelOsDatabase_IsRejected() {
        assertThatThrownBy(() -> MysqlTestDatabaseGuard.validate(
            "jdbc:mysql://localhost:3306/hostelos",
            "test_user",
            "test_pass"
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Database target safety guard violation")
            .hasMessageContaining("hostelos");
    }

    // 4. Rejection of other unexpected database names
    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:mysql://localhost:3306/hostelos_prod",
        "jdbc:mysql://localhost:3306/hostelos_dev",
        "jdbc:mysql://localhost:3306/hostelos_stage",
        "jdbc:mysql://localhost:3306/other_db",
        "jdbc:mysql://localhost:3306/hostelos_test_2",
        "jdbc:mysql://localhost:3306/hostelos_testing"
    })
    @DisplayName("Rejection of unexpected database names")
    void unexpectedDatabaseNames_AreRejected(String url) {
        assertThatThrownBy(() -> MysqlTestDatabaseGuard.validate(url, "test_user", "test_pass"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Database target safety guard violation");
    }

    // 5. Rejection of remote hostnames and IPs
    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:mysql://db.example.com:3306/hostelos_test",
        "jdbc:mysql://192.168.1.5:3306/hostelos_test",
        "jdbc:mysql://10.0.0.2:3306/hostelos_test",
        "jdbc:mysql://mysql-staging.internal:3306/hostelos_test"
    })
    @DisplayName("Rejection of remote hostnames and non-loopback IP addresses")
    void remoteHostnamesAndIps_AreRejected(String url) {
        assertThatThrownBy(() -> MysqlTestDatabaseGuard.validate(url, "test_user", "test_pass"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Database target safety guard violation");
    }

    // 6. Rejection of unexpected ports
    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:mysql://localhost:3307/hostelos_test",
        "jdbc:mysql://localhost:3308/hostelos_test",
        "jdbc:mysql://localhost:5432/hostelos_test",
        "jdbc:mysql://127.0.0.1:3307/hostelos_test"
    })
    @DisplayName("Rejection of unexpected ports (non-3306)")
    void unexpectedPorts_AreRejected(String url) {
        assertThatThrownBy(() -> MysqlTestDatabaseGuard.validate(url, "test_user", "test_pass"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Database target safety guard violation");
    }

    // 7. Rejection of malformed JDBC URLs
    @ParameterizedTest
    @ValueSource(strings = {
        "jdbc:postgresql://localhost:3306/hostelos_test",
        "jdbc:oracle:thin:@localhost:3306:hostelos_test",
        "not-a-valid-jdbc-url",
        "http://localhost:3306/hostelos_test",
        "",
        "   "
    })
    @DisplayName("Rejection of malformed or non-MySQL JDBC URLs")
    void malformedUrls_AreRejected(String url) {
        assertThatThrownBy(() -> MysqlTestDatabaseGuard.validate(url, "test_user", "test_pass"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Null URL is rejected with informative message")
    void nullUrl_IsRejected() {
        assertThatThrownBy(() -> MysqlTestDatabaseGuard.validate(null, "test_user", "test_pass"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("URL must not be null or blank");
    }

    // 8. Rejection of missing/blank credentials
    @Test
    @DisplayName("Null or blank username is rejected")
    void missingUsername_IsRejected() {
        assertThatThrownBy(() -> MysqlTestDatabaseGuard.validate(
            "jdbc:mysql://localhost:3306/hostelos_test", null, "test_pass"
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("username must not be null or blank");

        assertThatThrownBy(() -> MysqlTestDatabaseGuard.validate(
            "jdbc:mysql://localhost:3306/hostelos_test", "   ", "test_pass"
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("username must not be null or blank");
    }

    @Test
    @DisplayName("Null or blank password is rejected")
    void missingPassword_IsRejected() {
        assertThatThrownBy(() -> MysqlTestDatabaseGuard.validate(
            "jdbc:mysql://localhost:3306/hostelos_test", "test_user", null
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("password must not be null or blank");

        assertThatThrownBy(() -> MysqlTestDatabaseGuard.validate(
            "jdbc:mysql://localhost:3306/hostelos_test", "test_user", "   "
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("password must not be null or blank");
    }

    // 9. Context-level tests: fail-closed safety before any connection attempt
    @Test
    @DisplayName("Context initialization fails immediately if mysql-test profile targets unsafe database")
    void contextRunner_RejectsUnsafeTargetBeforeBeanCreation() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=mysql-test",
                "spring.datasource.url=jdbc:mysql://remote-db.production:3306/hostelos",
                "spring.datasource.username=root",
                "spring.datasource.password=secret"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Database target safety guard violation");
            });
    }

    @Test
    @DisplayName("Context initialization fails if mysql-test profile is active but credentials missing")
    void contextRunner_RejectsMissingCredentials() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=mysql-test",
                "spring.datasource.url=jdbc:mysql://localhost:3306/hostelos_test"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("username must not be null or blank");
            });
    }

    @Test
    @DisplayName("Context initialization succeeds when mysql-test profile is NOT active (e.g. H2 test profile)")
    void contextRunner_IgnoresWhenMysqlTestProfileNotActive() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=test",
                "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
            });
    }

    @Test
    @DisplayName("Context initialization succeeds when mysql-test profile is active with compliant configuration")
    void contextRunner_SucceedsWithCompliantConfiguration() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=mysql-test",
                "spring.datasource.url=jdbc:mysql://localhost:3306/hostelos_test?useSSL=false",
                "spring.datasource.username=test_user",
                "spring.datasource.password=test_pass"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
            });
    }

    @Test
    @DisplayName("URL sanitizer properly obscures credentials in query parameters")
    void sanitizeUrl_MasksSensitiveQueryParameters() {
        String raw = "jdbc:mysql://localhost:3306/hostelos_test?password=supersecret&user=root&secret=anothersecret";
        String sanitized = MysqlTestDatabaseGuard.sanitizeUrl(raw);

        assertThat(sanitized).doesNotContain("supersecret");
        assertThat(sanitized).doesNotContain("anothersecret");
        assertThat(sanitized).contains("password=***");
        assertThat(sanitized).contains("secret=***");
    }

    @Test
    @DisplayName("Environment validation directly resolves environment properties")
    void validateEnvironment_ResolvesDirectAndFallbackProperties() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("mysql-test");
        env.setProperty("spring.datasource.url", "jdbc:mysql://127.0.0.1:3306/hostelos_test");
        env.setProperty("spring.datasource.username", "test_user");
        env.setProperty("spring.datasource.password", "test_pass");

        assertThatCode(() -> MysqlTestDatabaseGuard.validateEnvironment(env))
            .doesNotThrowAnyException();
    }
}
