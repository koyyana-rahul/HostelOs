package com.rahul.HostelOs.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDatabaseProfileResolverTests {

    @Test
    @DisplayName("Defaults to H2 test profile when no selector is present")
    void defaultSelectionIsTest() {
        TestDatabaseProfileResolver resolver = new TestDatabaseProfileResolver(() -> null, () -> null);

        assertThat(resolver.resolve(Object.class)).containsExactly("test");
    }

    @Test
    @DisplayName("Selects test profile from the JVM property")
    void explicitTestSelectionViaProperty() {
        TestDatabaseProfileResolver resolver = new TestDatabaseProfileResolver(() -> "test", () -> null);

        assertThat(resolver.resolve(Object.class)).containsExactly("test");
    }

    @Test
    @DisplayName("Selects test profile from the environment variable")
    void explicitTestSelectionViaEnvironment() {
        TestDatabaseProfileResolver resolver = new TestDatabaseProfileResolver(() -> null, () -> "test");

        assertThat(resolver.resolve(Object.class)).containsExactly("test");
    }

    @Test
    @DisplayName("Selects mysql-test profile from the environment variable")
    void explicitMysqlTestSelectionViaEnvironment() {
        TestDatabaseProfileResolver resolver = new TestDatabaseProfileResolver(() -> null, () -> "mysql-test");

        assertThat(resolver.resolve(Object.class)).containsExactly("mysql-test");
    }

    @Test
    @DisplayName("JVM property takes precedence over the environment variable")
    void propertyTakesPrecedenceOverEnvironment() {
        TestDatabaseProfileResolver resolver = new TestDatabaseProfileResolver(
                () -> "mysql-test",
                () -> "test");

        assertThat(resolver.resolve(Object.class)).containsExactly("mysql-test");
    }

    @Test
    @DisplayName("Profile values are trimmed and case-normalized")
    void valuesAreTrimmedAndCaseNormalized() {
        TestDatabaseProfileResolver mysqlResolver = new TestDatabaseProfileResolver(
                () -> "  MySql-Test  ",
                () -> null);
        TestDatabaseProfileResolver testResolver = new TestDatabaseProfileResolver(
                () -> null,
                () -> "  TeSt  ");

        assertThat(mysqlResolver.resolve(Object.class)).containsExactly("mysql-test");
        assertThat(testResolver.resolve(Object.class)).containsExactly("test");
    }

    @Test
    @DisplayName("Blank JVM property is rejected without falling back to H2")
    void blankPropertyIsRejectedWithoutFallingBack() {
        TestDatabaseProfileResolver resolver = new TestDatabaseProfileResolver(() -> "   ", () -> "mysql-test");

        assertThatThrownBy(() -> resolver.resolve(Object.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported test profile")
                .hasMessageContaining("test")
                .hasMessageContaining("mysql-test");
    }

    @Test
    @DisplayName("Unsupported profile is rejected")
    void unsupportedProfileIsRejected() {
        TestDatabaseProfileResolver resolver = new TestDatabaseProfileResolver(() -> "production", () -> null);

        assertThatThrownBy(() -> resolver.resolve(Object.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported test profile")
                .hasMessageContaining("production");
    }
}
