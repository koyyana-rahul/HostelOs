package com.rahul.HostelOs.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hostelos.bootstrap.admin")
public record AdminBootstrapProperties(
		boolean enabled,
		String loginIdentifier,
		String displayName,
		String password) {
}
