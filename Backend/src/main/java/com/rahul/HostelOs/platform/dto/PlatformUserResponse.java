package com.rahul.HostelOs.platform.dto;

import java.util.List;

public record PlatformUserResponse(
		Long id,
		String loginIdentifier,
		String displayName,
		String status,
		List<String> roles) {
}
