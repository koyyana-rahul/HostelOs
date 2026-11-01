package com.rahul.HostelOs.customer;

import com.rahul.HostelOs.customer.dto.CustomerProfileResponse;
import com.rahul.HostelOs.customer.dto.UpdateCustomerProfileRequest;
import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.identity.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/customers/me")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerProfileController {

	private final UserAccountService userAccountService;
	private final SecurityContextRepository securityContextRepository;

	public CustomerProfileController(
			UserAccountService userAccountService,
			SecurityContextRepository securityContextRepository) {
		this.userAccountService = userAccountService;
		this.securityContextRepository = securityContextRepository;
	}

	@GetMapping
	public CustomerProfileResponse getProfile(@AuthenticationPrincipal HostelOsPrincipal principal) {
		return CustomerProfileResponse.from(userAccountService.getProfile(principal.getUserId()));
	}

	@PutMapping
	@ResponseStatus(HttpStatus.OK)
	public CustomerProfileResponse updateProfile(
			@AuthenticationPrincipal HostelOsPrincipal principal,
			@Valid @RequestBody UpdateCustomerProfileRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		var updatedAccount = userAccountService.updateDisplayName(principal.getUserId(), request.displayName());
		HostelOsPrincipal updatedPrincipal = HostelOsPrincipal.from(updatedAccount);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(
				updatedPrincipal,
				null,
				updatedPrincipal.getAuthorities()));
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);
		return CustomerProfileResponse.from(updatedAccount);
	}
}
