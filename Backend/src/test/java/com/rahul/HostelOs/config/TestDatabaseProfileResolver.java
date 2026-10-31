package com.rahul.HostelOs.config;

import java.util.Locale;
import java.util.function.Supplier;
import org.springframework.test.context.ActiveProfilesResolver;

public final class TestDatabaseProfileResolver implements ActiveProfilesResolver {

    static final String PROFILE_PROPERTY = "hostelos.test.profile";
    static final String PROFILE_ENVIRONMENT_VARIABLE = "HOSTELOS_TEST_PROFILE";
    private static final String TEST_PROFILE = "test";
    private static final String MYSQL_TEST_PROFILE = "mysql-test";

    private final Supplier<String> propertyValue;
    private final Supplier<String> environmentValue;

    public TestDatabaseProfileResolver() {
        this(() -> System.getProperty(PROFILE_PROPERTY), () -> System.getenv(PROFILE_ENVIRONMENT_VARIABLE));
    }

    TestDatabaseProfileResolver(Supplier<String> propertyValue, Supplier<String> environmentValue) {
        this.propertyValue = propertyValue;
        this.environmentValue = environmentValue;
    }

    @Override
    public String[] resolve(Class<?> testClass) {
        String requested = propertyValue.get();
        if (requested == null) {
            requested = environmentValue.get();
        }
        if (requested == null) {
            requested = TEST_PROFILE;
        }
        return resolveProfile(requested);
    }

    private static String[] resolveProfile(String requested) {
        String normalized = requested.trim().toLowerCase(Locale.ROOT);
        if (TEST_PROFILE.equals(normalized)) {
            return new String[] { TEST_PROFILE };
        }
        if (MYSQL_TEST_PROFILE.equals(normalized)) {
            return new String[] { MYSQL_TEST_PROFILE };
        }
        throw new IllegalStateException(
                "Unsupported test profile: '" + requested + "'. "
                        + "Allowed values are '" + TEST_PROFILE + "' and '" + MYSQL_TEST_PROFILE + "'. "
                        + "Select one with -D" + PROFILE_PROPERTY + "=<profile> or "
                        + PROFILE_ENVIRONMENT_VARIABLE + "=<profile>.");
    }
}
