package com.rahul.HostelOs.customer.dto;

import com.rahul.HostelOs.identity.UserAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Profile information for the authenticated customer.")
public record CustomerProfileResponse(
		Long id,
		String loginIdentifier,
		String displayName,
		List<String> roles) {

	public static CustomerProfileResponse from(UserAccount account) {
		return new CustomerProfileResponse(
				account.getId(),
				account.getLoginIdentifier(),
				account.getDisplayName(),
				account.getRoles().stream().map(role -> role.getName().name()).sorted().toList());
	}
}
