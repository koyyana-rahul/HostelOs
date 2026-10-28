package com.rahul.HostelOs.identity;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HostelOsUserDetailsService implements UserDetailsService {

	private final UserAccountRepository userAccounts;

	public HostelOsUserDetailsService(UserAccountRepository userAccounts) {
		this.userAccounts = userAccounts;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		String loginIdentifier;
		try {
			loginIdentifier = LoginIdentifier.normalize(username);
		} catch (RuntimeException exception) {
			throw new UsernameNotFoundException("Authentication failed");
		}
		UserAccount account = userAccounts.findByLoginIdentifier(loginIdentifier)
				.orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
		return HostelOsPrincipal.from(account);
	}
}
