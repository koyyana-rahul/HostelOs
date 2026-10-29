package com.rahul.HostelOs.auth;

import com.rahul.HostelOs.common.exception.ApiError;
import com.rahul.HostelOs.identity.HostelOsPrincipal;
import com.rahul.HostelOs.identity.LoginAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.validation.BindingResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Session authentication and identity")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final LoginAttemptService loginAttempts;

	public AuthController(
			AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository,
			SessionAuthenticationStrategy sessionAuthenticationStrategy,
			LoginAttemptService loginAttempts) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.loginAttempts = loginAttempts;
	}

	@Operation(summary = "Bootstrap a CSRF token")
	@ApiResponse(responseCode = "200", description = "CSRF token returned")
	@GetMapping("/csrf")
	public CsrfResponse csrf(HttpServletRequest request) {
		CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		if (token == null) {
			token = (CsrfToken) request.getAttribute("_csrf");
		}
		if (token == null) {
			throw new IllegalStateException("CSRF token is not available");
		}
		return new CsrfResponse(token.getHeaderName(), token.getToken());
	}

	@Operation(summary = "Authenticate and create a session")
	@ApiResponse(responseCode = "200", description = "Authentication succeeded")
	@ApiResponse(responseCode = "400", description = "Invalid login request")
	@ApiResponse(responseCode = "401", description = "Authentication failed")
	@ApiResponse(responseCode = "429", description = "Too many authentication attempts")
	@PostMapping("/login")
	public ResponseEntity<Object> login(
			@Valid @RequestBody LoginRequest loginRequest,
			HttpServletRequest request,
			HttpServletResponse response) {
		String remoteAddress = request.getRemoteAddr();
		// Reject passwords exceeding 72 UTF-8 bytes; this aligns with
		// UserAccountService.createUser check and BCrypt's internal limit.
		// Placed early so it is evaluated before any authentication attempt.
		String password = loginRequest.password();
		if (password != null && password.getBytes(StandardCharsets.UTF_8).length > 72) {
			return ResponseEntity
					.badRequest()
					.body(ApiError.of(HttpStatus.BAD_REQUEST,
							"Password must not exceed 72 UTF-8 bytes",
							request.getRequestURI()));
		}
		if (loginAttempts.isBlocked(loginRequest.loginIdentifier(), remoteAddress)) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.body(ApiError.of(HttpStatus.TOO_MANY_REQUESTS, "Too many authentication attempts", request.getRequestURI()));
		}
		try {
			Authentication authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(
							loginRequest.loginIdentifier(),
							loginRequest.password()));
			loginAttempts.recordSuccess(loginRequest.loginIdentifier(), remoteAddress);
			request.getSession(true);
			sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			securityContextRepository.saveContext(context, request, response);
			return ResponseEntity.ok(AuthUserResponse.from((HostelOsPrincipal) authentication.getPrincipal()));
		} catch (AuthenticationException exception) {
			loginAttempts.recordFailure(loginRequest.loginIdentifier(), remoteAddress);
			SecurityContextHolder.clearContext();
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(ApiError.of(HttpStatus.UNAUTHORIZED, "Invalid credentials", request.getRequestURI()));
		}
	}

	@Operation(summary = "Invalidate the current session")
	@ApiResponse(responseCode = "204", description = "Logout completed")
	@SecurityRequirement(name = "sessionAuth")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Get the current identity")
	@ApiResponse(responseCode = "200", description = "Current identity returned")
	@ApiResponse(responseCode = "401", description = "Authentication is required")
	@SecurityRequirement(name = "sessionAuth")
	@GetMapping("/me")
	public AuthUserResponse me(@AuthenticationPrincipal HostelOsPrincipal principal) {
		return AuthUserResponse.from(principal);
	}
}
