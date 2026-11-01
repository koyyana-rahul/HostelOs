package com.rahul.HostelOs.identity;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

	private final UserAccountRepository userAccounts;
	private final RoleRepository roles;
	private final PasswordEncoder passwordEncoder;

	public UserAccountService(
			UserAccountRepository userAccounts,
			RoleRepository roles,
			PasswordEncoder passwordEncoder) {
		this.userAccounts = userAccounts;
		this.roles = roles;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserAccount createUser(
			String rawLoginIdentifier,
			String displayName,
			String rawPassword,
			Set<RoleName> roleNames) {
		String loginIdentifier = LoginIdentifier.normalize(rawLoginIdentifier);
		String normalizedDisplayName = displayName == null ? "" : displayName.trim();
		if (loginIdentifier.length() < 3 || loginIdentifier.length() > 254) {
			throw new IllegalArgumentException("Login identifier must be between 3 and 254 characters");
		}
		if (normalizedDisplayName.isBlank()) {
			throw new IllegalArgumentException("Display name must not be blank");
		}
		if (normalizedDisplayName.length() > 120) {
			throw new IllegalArgumentException("Display name must not exceed 120 characters");
		}
		if (rawPassword == null || rawPassword.isBlank()) {
			throw new IllegalArgumentException("Password must not be blank");
		}
		if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes");
		}
		if (roleNames == null || roleNames.isEmpty()) {
			throw new IllegalArgumentException("At least one role is required");
		}
		if (userAccounts.existsByLoginIdentifier(loginIdentifier)) {
			throw new DuplicateLoginIdentifierException();
		}
		Set<Role> assignedRoles = new HashSet<>();
		for (RoleName roleName : roleNames) {
			assignedRoles.add(roles.findByName(roleName)
					.orElseThrow(() -> new IllegalStateException("Required role is missing: " + roleName)));
		}
		UserAccount account = new UserAccount(
				loginIdentifier,
				normalizedDisplayName,
				passwordEncoder.encode(rawPassword),
				AccountStatus.ACTIVE,
				assignedRoles);
		return userAccounts.save(account);
	}

	@Transactional
	public UserAccount registerCustomer(String rawLoginIdentifier, String displayName, String rawPassword) {
		return createUser(rawLoginIdentifier, displayName, rawPassword, Set.of(RoleName.CUSTOMER));
	}

	@Transactional
	public UserAccount registerOwner(String rawLoginIdentifier, String displayName, String rawPassword) {
		return createUser(rawLoginIdentifier, displayName, rawPassword, Set.of(RoleName.ORGANIZATION_ADMIN));
	}

	@Transactional
	public boolean createPlatformAdminIfAbsent(String rawLoginIdentifier, String displayName, String rawPassword) {
		String loginIdentifier = LoginIdentifier.normalize(rawLoginIdentifier);
		if (userAccounts.existsByLoginIdentifier(loginIdentifier)) {
			return false;
		}
		createUser(loginIdentifier, displayName, rawPassword, Set.of(RoleName.PLATFORM_ADMIN));
		return true;
	}

	@Transactional(readOnly = true)
	public UserAccount getProfile(Long userId) {
		return userAccounts.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User account was not found"));
	}

	@Transactional
	public UserAccount updateDisplayName(Long userId, String displayName) {
		UserAccount account = getProfile(userId);
		String normalizedDisplayName = normalizeDisplayName(displayName);
		account.updateDisplayName(normalizedDisplayName);
		return userAccounts.saveAndFlush(account);
	}

	private String normalizeDisplayName(String displayName) {
		String normalized = displayName == null ? "" : displayName.trim();
		if (normalized.isBlank()) {
			throw new IllegalArgumentException("Display name must not be blank");
		}
		if (normalized.length() > 120) {
			throw new IllegalArgumentException("Display name must not exceed 120 characters");
		}
		return normalized;
	}
}
