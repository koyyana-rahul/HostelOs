package com.rahul.HostelOs.security;

import com.rahul.HostelOs.identity.ActiveAccountFilter;
import com.rahul.HostelOs.identity.AdminBootstrapProperties;
import com.rahul.HostelOs.identity.UserAccountRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class SecurityConfig {

	private final boolean secureCookies;

	public SecurityConfig(@Value("${hostelos.security.cookie-secure:false}") boolean secureCookies) {
		this.secureCookies = secureCookies;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ApiSecurityErrorHandler errorHandler,
			SecurityContextRepository securityContextRepository,
			CsrfTokenRepository csrfTokenRepository,
			UserAccountRepository userAccountRepository) throws Exception {
		ActiveAccountFilter activeAccountFilter = new ActiveAccountFilter(userAccountRepository);
		http
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository)
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
						.requireCsrfProtectionMatcher(csrfProtectionMatcher()))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.requestCache(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.sessionFixation(fixation -> fixation.changeSessionId()))
				.securityContext(context -> context
						.securityContextRepository(securityContextRepository))
				.authorizeHttpRequests(requests -> requests
				.requestMatchers(HttpMethod.GET, "/api/v1/system/info").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/register/owner").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/listings/*/enquiries").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/v1/auth/me/memberships").authenticated()
				.requestMatchers("/api/v1/customers/**").authenticated()
				.requestMatchers("/api/v1/auth/**").denyAll()
						.requestMatchers(
								"/actuator/health",
								"/actuator/health/**",
								"/v3/api-docs",
								"/v3/api-docs/**",
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/error")
						.permitAll()
						.requestMatchers("/api/v1/organizations/**").authenticated()
						.requestMatchers("/api/v1/platform/**").hasRole("PLATFORM_ADMIN")
						.anyRequest().denyAll())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler))
				.addFilterAfter(activeAccountFilter, SecurityContextHolderFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	public AuthenticationManager authenticationManager(
			UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();
		repository.setAllowSessionCreation(true);
		return repository;
	}

	@Bean
	public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new ChangeSessionIdAuthenticationStrategy();
	}

	@Bean
	public CsrfTokenRepository csrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie
				.path("/")
				.sameSite("Lax")
				.secure(secureCookies));
		return repository;
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	private RequestMatcher csrfProtectionMatcher() {
		return request -> {
			String method = request.getMethod();
			if ("GET".equals(method) || "HEAD".equals(method) || "TRACE".equals(method) || "OPTIONS".equals(method)) {
				return false;
			}
			String path = request.getRequestURI().substring(request.getContextPath().length());
			if ("POST".equals(method) && ("/api/v1/auth/login".equals(path)
					|| "/api/v1/auth/register".equals(path)
					|| "/api/v1/auth/register/owner".equals(path))) {
				return true;
			}
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			return authentication != null
					&& authentication.isAuthenticated()
					&& !(authentication instanceof AnonymousAuthenticationToken);
		};
	}
}
