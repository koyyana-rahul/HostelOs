package com.rahul.HostelOs.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminBootstrapRunnerTests {

	private static final String PASSWORD = "bootstrap-password-1234";

	private final UserAccountService userAccounts = mock(UserAccountService.class);

	@Test
	void disabledBootstrapDoesNothing() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
				new AdminBootstrapProperties(false, "admin", "Administrator", PASSWORD),
				userAccounts);

		runner.run(null);

		verifyNoInteractions(userAccounts);
	}

	@Test
	void incompleteConfigurationFailsStartup() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
				new AdminBootstrapProperties(true, "", "Administrator", PASSWORD),
				userAccounts);

		assertThatThrownBy(() -> runner.run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("incomplete");
	}

	@Test
	void blankDisplayNameFailsStartup() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
				new AdminBootstrapProperties(true, "admin", "", PASSWORD),
				userAccounts);

		assertThatThrownBy(() -> runner.run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("incomplete");
	}

	@Test
	void blankPasswordFailsStartup() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
				new AdminBootstrapProperties(true, "admin", "Administrator", ""),
				userAccounts);

		assertThatThrownBy(() -> runner.run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("incomplete");
	}

	@Test
	void shortPasswordFailsStartup() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
				new AdminBootstrapProperties(true, "admin", "Administrator", "too-short"),
				userAccounts);

		assertThatThrownBy(() -> runner.run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 16");
	}

	@Test
	void validConfigurationCreatesTheBootstrapAdministrator() {
		when(userAccounts.createPlatformAdminIfAbsent("admin", "Administrator", PASSWORD)).thenReturn(true);
		AdminBootstrapRunner runner = new AdminBootstrapRunner(
				new AdminBootstrapProperties(true, "admin", "Administrator", PASSWORD),
				userAccounts);

		runner.run(null);

		verify(userAccounts).createPlatformAdminIfAbsent("admin", "Administrator", PASSWORD);
	}
}
