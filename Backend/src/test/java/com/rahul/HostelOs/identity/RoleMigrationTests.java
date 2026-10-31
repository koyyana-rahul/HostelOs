package com.rahul.HostelOs.identity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleMigrationTests {

	private static final String PASSWORD_HASH = "$2a$12$01234567890123456789012345678901234567890123456789012";

	@TempDir
	private Path tempDir;

	@Test
	void freshDatabaseSeedsTheReconciledRoles() throws Exception {
		String url = databaseUrl();

		migrate(url, null);

		try (Connection connection = connection(url)) {
			assertThat(roleNames(connection))
					.containsExactly("CUSTOMER", "ORGANIZATION_ADMIN", "PLATFORM_ADMIN", "STAFF");
		}
	}

	@Test
	void upgradesV2RoleAssignmentsWithoutLosingIdentityData() throws Exception {
		String url = databaseUrl();
		migrate(url, "2");

		long platformAdminId;
		long platformAdminRoleId;
		long staffId;
		long staffRoleId;
		try (Connection connection = connection(url)) {
			platformAdminId = insertUser(connection, "legacy-admin@example.com", "Legacy Administrator");
			staffId = insertUser(connection, "legacy-staff@example.com", "Legacy Staff");
			platformAdminRoleId = roleId(connection, "ADMIN");
			staffRoleId = roleId(connection, "STAFF");
			assignRole(connection, platformAdminId, platformAdminRoleId);
			assignRole(connection, staffId, staffRoleId);
		}

		migrate(url, null);

		try (Connection connection = connection(url)) {
			assertThat(roleNames(connection))
					.containsExactly("CUSTOMER", "ORGANIZATION_ADMIN", "PLATFORM_ADMIN", "STAFF");
			assertThat(count(connection, "SELECT COUNT(*) FROM user_accounts")).isEqualTo(2);
			assertThat(count(connection, "SELECT COUNT(*) FROM user_roles")).isEqualTo(2);
			assertThat(assignedRoleNames(connection, platformAdminId)).containsExactly("PLATFORM_ADMIN");
			assertThat(assignedRoleNames(connection, staffId)).containsExactly("STAFF");
			assertThat(roleId(connection, "PLATFORM_ADMIN")).isEqualTo(platformAdminRoleId);
			assertThat(roleId(connection, "STAFF")).isEqualTo(staffRoleId);

			assertThatThrownBy(() -> insertLegacyAdminRole(connection))
					.isInstanceOf(SQLException.class);
		}
	}

	private String databaseUrl() {
		String databasePath = tempDir.resolve("identity").toAbsolutePath().toString().replace('\\', '/');
		return "jdbc:h2:file:" + databasePath + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
	}

	private void migrate(String url, String target) {
		var configuration = Flyway.configure()
				.dataSource(url, "sa", "")
				.locations("classpath:db/migration")
				.cleanDisabled(true);
		if (target != null) {
			configuration.target(target);
		}
		configuration.load().migrate();
	}

	private Connection connection(String url) throws SQLException {
		return DriverManager.getConnection(url, "sa", "");
	}

	private long insertUser(Connection connection, String login, String displayName) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO user_accounts (login_identifier, display_name, password_hash, status) "
						+ "VALUES (?, ?, ?, 'ACTIVE')",
				Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, login);
			statement.setString(2, displayName);
			statement.setString(3, PASSWORD_HASH);
			statement.executeUpdate();
			try (ResultSet keys = statement.getGeneratedKeys()) {
				assertThat(keys.next()).isTrue();
				return keys.getLong(1);
			}
		}
	}

	private long roleId(Connection connection, String roleName) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM roles WHERE name = ?")) {
			statement.setString(1, roleName);
			try (ResultSet resultSet = statement.executeQuery()) {
				assertThat(resultSet.next()).as("Role %s exists", roleName).isTrue();
				return resultSet.getLong(1);
			}
		}
	}

	private void assignRole(Connection connection, long userId, long roleId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
			statement.setLong(1, userId);
			statement.setLong(2, roleId);
			statement.executeUpdate();
		}
	}

	private List<String> roleNames(Connection connection) throws SQLException {
		List<String> names = new ArrayList<>();
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT name FROM roles ORDER BY name")) {
			while (resultSet.next()) {
				names.add(resultSet.getString(1));
			}
		}
		return names;
	}

	private List<String> assignedRoleNames(Connection connection, long userId) throws SQLException {
		List<String> names = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"SELECT r.name FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? ORDER BY r.name")) {
			statement.setLong(1, userId);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					names.add(resultSet.getString(1));
				}
			}
		}
		return names;
	}

	private long count(Connection connection, String sql) throws SQLException {
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql)) {
			assertThat(resultSet.next()).isTrue();
			return resultSet.getLong(1);
		}
	}

	private void insertLegacyAdminRole(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO roles (name, description) VALUES ('ADMIN', 'Legacy role')");
		}
	}
}
