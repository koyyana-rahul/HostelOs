package com.rahul.HostelOs.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rahul.HostelOs.customer.dto.CustomerProfileResponse;
import com.rahul.HostelOs.customer.dto.UpdateCustomerProfileRequest;
import com.rahul.HostelOs.identity.AccountStatus;
import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.identity.Role;
import com.rahul.HostelOs.identity.RoleName;
import com.rahul.HostelOs.identity.UserAccount;
import com.rahul.HostelOs.identity.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

@ExtendWith(MockitoExtension.class)
class CustomerProfileControllerTests {

	@Mock
	private UserAccountService userAccounts;

	@Mock
	private SecurityContextRepository securityContextRepository;

	@Mock
	private HostelOsPrincipal principal;

	@Mock
	private Role customerRole;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Tes
	void profileUpdateRefreshesTheAuthenticatedPrincipal() {
		UserAccount updated = new UserAccount(
				"customer@example.com",
				"Updated Customer",
				"hash",
				AccountStatus.ACTIVE,
				java.util.Set.of(customerRole));
		when(principal.getUserId()).thenReturn(42L);
		when(customerRole.getName()).thenReturn(RoleName.CUSTOMER);
		when(userAccounts.updateDisplayName(42L, "Updated Customer")).thenReturn(updated);

		CustomerProfileController controller =
				new CustomerProfileController(userAccounts, securityContextRepository);
		CustomerProfileResponse responseBody = controller.updateProfile(
				principal,
				new UpdateCustomerProfileRequest("Updated Customer"),
				request,
				response);

		assertThat(responseBody.displayName()).isEqualTo("Updated Customer");
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.isInstanceOf(HostelOsPrincipal.class)
				.extracting(HostelOsPrincipal.class::cast)
				.extracting(HostelOsPrincipal::getDisplayName)
				.isEqualTo("Updated Customer");
		verify(securityContextRepository).saveContext(
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.same(request),
				org.mockito.ArgumentMatchers.same(response));
	}
}
