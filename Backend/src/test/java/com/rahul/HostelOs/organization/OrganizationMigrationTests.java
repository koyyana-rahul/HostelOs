package com.rahul.HostelOs.organization;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationMigrationTests {

	@TempDir
	private Path tempDir;

	@Test
	void appliesV4MigrationAndCreatesExpectedSchema() throws Exception {
		String url = databaseUrl();

		migrate(url);

		try (Connection connection = connection(url)) {
			// Check tables exist
			List<String> tables = tableNames(connection);
			assertThat(tables).contains("ORGANIZATIONS", "ORGANIZATION_MEMBERSHIPS", "ROLES", "USER_ACCOUNTS", "USER_ROLES");

			// Insert valid organization
			long orgId = insertOrganization(connection, "Acme Hostels", "acme-hostels", "ACTIVE");
			assertThat(orgId).isPositive();

			// Insert user
			long userId = insertUser(connection, "admin@acme.com", "Acme Admin");
			assertThat(userId).isPositive();

			// Insert valid membership
			long memberId = insertMembership(connection, orgId, userId, "ORG_ADMIN", "ACTIVE");
			assertThat(memberId).isPositive();

			// Reject duplicate slug
			assertThatThrownBy(() -> insertOrganization(connection, "Acme Duplicate", "acme-hostels", "ACTIVE"))
					.isInstanceOf(SQLException.class);

			// Reject duplicate membership
			assertThatThrownBy(() -> insertMembership(connection, orgId, userId, "ORG_STAFF", "ACTIVE"))
					.isInstanceOf(SQLException.class);

			// Reject invalid org status
			assertThatThrownBy(() -> insertOrganization(connection, "Bad Status", "bad-status", "UNKNOWN"))
					.isInstanceOf(SQLException.class);

			// Reject invalid membership role
			assertThatThrownBy(() -> insertMembership(connection, orgId, userId, "UNKNOWN_ROLE", "ACTIVE"))
					.isInstanceOf(SQLException.class);
		}
	}

	private String databaseUrl() {
		String databasePath = tempDir.resolve("migration-test").toAbsolutePath().toString().replace('\\', '/');
		return "jdbc:h2:file:" + databasePath + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
	}

	private void migrate(String url) {
		Flyway.configure()
				.dataSource(url, "sa", "")
				.locations("classpath:db/migration")
				.cleanDisabled(true)
				.load()
				.migrate();
	}

	private Connection connection(String url) throws SQLException {
		return DriverManager.getConnection(url, "sa", "");
	}

	private List<String> tableNames(Connection connection) throws SQLException {
		List<String> names = new ArrayList<>();
		try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
			while (rs.next()) {
				names.add(rs.getString("TABLE_NAME").toUpperCase());
			}
		}
		return names;
	}

	private long insertOrganization(Connection connection, String name, String slug, String status) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO organizations (name, slug, status) VALUES (?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, name);
			stmt.setString(2, slug);
			stmt.setString(3, status);
			stmt.executeUpdate();
			try (ResultSet keys = stmt.getGeneratedKeys()) {
				assertThat(keys.next()).isTrue();
				return keys.getLong(1);
			}
		}
	}

	private long insertUser(Connection connection, String login, String displayName) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO user_accounts (login_identifier, display_name, password_hash, status) VALUES (?, ?, ?, 'ACTIVE')",
				Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, login);
			stmt.setString(2, displayName);
			stmt.setString(3, "$2a$12$01234567890123456789012345678901234567890123456789012");
			stmt.executeUpdate();
			try (ResultSet keys = stmt.getGeneratedKeys()) {
				assertThat(keys.next()).isTrue();
				return keys.getLong(1);
			}
		}
	}

	private long insertMembership(Connection connection, long orgId, long userId, String role, String status) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO organization_memberships (organization_id, user_id, org_role, status) VALUES (?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS)) {
			stmt.setLong(1, orgId);
			stmt.setLong(2, userId);
			stmt.setString(3, role);
			stmt.setString(4, status);
			stmt.executeUpdate();
			try (ResultSet keys = stmt.getGeneratedKeys()) {
				assertThat(keys.next()).isTrue();
				return keys.getLong(1);
			}
		}
	}
}
