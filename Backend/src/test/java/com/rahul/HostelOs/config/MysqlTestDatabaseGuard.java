package com.rahul.HostelOs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fail-closed database safety guard for the 'mysql-test' profile.
 * <p>
 * Ensures that tests running against MySQL strictly target a local, dedicated test database
 * ('hostelos_test' on localhost/127.0.0.1:3306) and reject production, development, remote,
 * or non-conforming database instances.
 * <p>
 * Implemented as an {@link ApplicationContextInitializer} so that verification executes
 * during application context preparation before any bean definitions, DataSources, Flyway,
 * or Hibernate connections can be initialized or attempted.
 */
public class MysqlTestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(MysqlTestDatabaseGuard.class);

    public static final String MYSQL_TEST_PROFILE = "mysql-test";

    /**
     * Strict regex allowing only localhost or 127.0.0.1 on port 3306 with database 'hostelos_test',
     * optionally followed by query parameters or fragment identifiers.
     */
    public static final Pattern VALID_TEST_DB_URL_PATTERN = Pattern.compile(
        "^jdbc:mysql://(localhost|127\\.0\\.0\\.1):3306/hostelos_test([?#].*)?$"
    );

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        if (isMysqlTestProfileActive(environment)) {
            log.info("Active profile '{}' detected. Executing fail-closed database safety guard...", MYSQL_TEST_PROFILE);
            validateEnvironment(environment);
        }
    }

    /**
     * Checks if the 'mysql-test' profile is active in the given environment.
     */
    public static boolean isMysqlTestProfileActive(Environment environment) {
        if (environment == null) {
            return false;
        }
        return environment.acceptsProfiles(Profiles.of(MYSQL_TEST_PROFILE));
    }

    /**
     * Validates database properties configured in the Spring environment.
     *
     * @param environment the Spring environment
     * @throws IllegalStateException if configuration fails safety validation
     */
    public static void validateEnvironment(Environment environment) {
        if (environment == null) {
            throw new IllegalStateException("Spring Environment must not be null.");
        }

        String url = resolveProperty(environment, "spring.datasource.url", "HOSTELOS_TEST_DB_URL");
        String username = resolveProperty(environment, "spring.datasource.username", "HOSTELOS_TEST_DB_USERNAME");
        String password = resolveProperty(environment, "spring.datasource.password", "HOSTELOS_TEST_DB_PASSWORD");

        validate(url, username, password);
    }

    /**
     * Validates database connection URL and credentials against fail-closed safety constraints.
     *
     * @param url      the JDBC URL
     * @param username the database username
     * @param password the database password
     * @throws IllegalStateException if URL or credentials violate safety rules
     */
    public static void validate(String url, String username, String password) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalStateException("MySQL test database URL must not be null or blank.");
        }

        String trimmedUrl = url.trim();
        Matcher matcher = VALID_TEST_DB_URL_PATTERN.matcher(trimmedUrl);
        if (!matcher.matches()) {
            String sanitized = sanitizeUrl(trimmedUrl);
            throw new IllegalStateException(
                "Database target safety guard violation: URL [" + sanitized + "] is not permitted. "
                    + "Target database must strictly be 'hostelos_test' on localhost or 127.0.0.1 and port 3306. "
                    + "Pattern required: " + VALID_TEST_DB_URL_PATTERN.pattern()
            );
        }

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalStateException("MySQL test database username must not be null or blank.");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException("MySQL test database password must not be null or blank.");
        }

        log.info("MySQL test database safety guard PASSED: target database is verified as local hostelos_test ({})", sanitizeUrl(trimmedUrl));
    }

    /**
     * Sanitizes a JDBC URL to prevent credentials or sensitive parameters from being logged.
     */
    public static String sanitizeUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.replaceAll("(?i)(password|pwd|secret)=[^&;#]*", "$1=***");
    }

    private static String resolveProperty(Environment environment, String springKey, String envKey) {
        try {
            String val = environment.getProperty(springKey);
            if (val != null && !val.trim().isEmpty() && !val.startsWith("${")) {
                return val.trim();
            }
        } catch (Exception ignored) {
            // Placeholder unresolvable or property missing
        }
        try {
            String val = environment.getProperty(envKey);
            if (val != null && !val.trim().isEmpty() && !val.startsWith("${")) {
                return val.trim();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
