package com.rahul.HostelOs.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountRepository;
import com.rahul.HostelOs.identity.UserAccountService;
import java.util.Set;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTests {

	@Mock
	private UserAccountRepository userAccounts;

	@Mock
	private com.rahul.HostelOs.identity.RoleRepository roles;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Test
	void updatesOnlyThePermittedDisplayNameField() {
		UserAccount account = new UserAccount(
				"customer@example.com",
				"Original Name",
				"hash",
				AccountStatus.ACTIVE,
				Set.of());
		when(userAccounts.findById(42L)).thenReturn(Optional.of(account));
		when(userAccounts.saveAndFlush(account)).thenReturn(account);

		UserAccountService service = new UserAccountService(userAccounts, roles, passwordEncoder);
		UserAccount updated = service.updateDisplayName(42L, " Updated Name ");

		assertThat(updated.getDisplayName()).isEqualTo("Updated Name");
		assertThat(updated.getLoginIdentifier()).isEqualTo("customer@example.com");
		assertThat(updated.getRoles()).isEmpty();
		verify(userAccounts).saveAndFlush(account);
	}

	@Test
	void rejectsBlankDisplayNameUpdates() {
		UserAccount account = new UserAccount(
				"customer@example.com",
				"Original Name",
				"hash",
				AccountStatus.ACTIVE,
				Set.of());
		when(userAccounts.findById(42L)).thenReturn(Optional.of(account));

		UserAccountService service = new UserAccountService(userAccounts, roles, passwordEncoder);

		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> service.updateDisplayName(42L, "  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Display name must not be blank");
	}

	@Test
	void rejectsOverlongDisplayNameUpdates() {
		UserAccount account = new UserAccount(
				"customer@example.com", "Original Name", "hash", AccountStatus.ACTIVE, Set.of());
		when(userAccounts.findById(42L)).thenReturn(Optional.of(account));

		UserAccountService service = new UserAccountService(userAccounts, roles, passwordEncoder);

		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> service.updateDisplayName(42L, "x".repeat(121)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Display name must not exceed 120 characters");
	}
}
