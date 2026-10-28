package com.rahul.HostelOs.identity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class HostelOsPrincipal implements UserDetails, CredentialsContainer, Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final Long userId;
	private final String loginIdentifier;
	private final String displayName;
	private final List<String> roles;
	private final Set<GrantedAuthority> authorities;
	private final boolean enabled;
	private transient String passwordHash;

	private HostelOsPrincipal(
			Long userId,
			String loginIdentifier,
			String displayName,
			List<String> roles,
			boolean enabled,
			String passwordHash) {
		this.userId = userId;
		this.loginIdentifier = loginIdentifier;
		this.displayName = displayName;
		this.roles = List.copyOf(roles);
		this.enabled = enabled;
		this.passwordHash = passwordHash;
		this.authorities = roles.stream()
				.map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	public static HostelOsPrincipal from(UserAccount account) {
		List<String> roleNames = account.getRoles().stream()
				.map(role -> role.getName().name())
				.sorted()
				.toList();
		return new HostelOsPrincipal(
				account.getId(),
				account.getLoginIdentifier(),
				account.getDisplayName(),
				roleNames,
				account.getStatus() == AccountStatus.ACTIVE,
				account.getPasswordHash());
	}

	public Long getUserId() {
		return userId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public List<String> getRoles() {
		return roles;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return loginIdentifier;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void eraseCredentials() {
		this.passwordHash = null;
	}
}
