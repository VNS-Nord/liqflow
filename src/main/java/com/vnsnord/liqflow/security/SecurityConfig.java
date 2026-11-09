package com.vnsnord.liqflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * <p>Wires session-based authentication, method security, CSRF protection, and
 * the security-context storage used by the authentication controller. Public
 * access is limited to login and CSRF-token retrieval; every other request
 * must be authenticated.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig
{
	/**
	 * Creates the authentication manager backed by a DAO provider and the
	 * application's {@link UserDetailsService} and {@link PasswordEncoder}.
	 *
	 * @param userDetailsService the service used to load user details
	 * @param passwordEncoder    the encoder used to verify passwords
	 * @return the authentication manager
	 */
	@Bean
	public AuthenticationManager authentificationManager(UserDetailsService userDetailsService,
	                                                     PasswordEncoder passwordEncoder)
	{
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
		authenticationProvider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(authenticationProvider);
	}

	/**
	 * Builds the main security filter chain, including authorization rules,
	 * CSRF protection, session management, and JSON 401/403 error responses.
	 *
	 * @param http                      the HTTP security builder
	 * @param securityContextRepository the repository used to persist the security context
	 * @param csrfTokenRepository       the repository used to persist CSRF tokens
	 * @return the configured security filter chain
	 * @throws Exception if the filter chain cannot be built
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
	                                               SecurityContextRepository securityContextRepository,
												   CsrfTokenRepository csrfTokenRepository) throws Exception
	{
		http.authorizeHttpRequests(authorise -> authorise.requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
														 .permitAll()
														 .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf")
														 .permitAll().anyRequest().authenticated())
			.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
			.securityContext(context -> context.securityContextRepository(securityContextRepository)
											   .requireExplicitSave(true))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
				response.setStatus(401);
				response.setContentType("application/json");
				response.getWriter().write("{\"message\":\"Authentication required\"}");
			}).accessDeniedHandler((request, response, exception) -> {
				response.setStatus(403);
				response.setContentType("application/json");
				response.getWriter().write("{\"message\":\"Access denied\"}");
			})).logout(logout -> logout.logoutUrl("/api/v1/auth/logout").invalidateHttpSession(true)
									   .clearAuthentication(true).deleteCookies("JSESSIONID")
									   .logoutSuccessHandler((request, response, authentication) -> response.setStatus(
				                               204)));
		return http.build();
	}

	/**
	 * Provides the security-context repository that stores the context both as a
	 * request attribute and in the HTTP session.
	 *
	 * @return the security-context repository
	 */
	@Bean
	public SecurityContextRepository securityContextRepository()
	{
		return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
		                                               new HttpSessionSecurityContextRepository());
	}

	/**
	 * Provides the CSRF token repository backed by the HTTP session.
	 *
	 * @return the CSRF token repository
	 */
	@Bean
	public CsrfTokenRepository csrfTokenRepository()
	{
		return new HttpSessionCsrfTokenRepository();
	}

	/**
	 * Provides the session authentication strategy that rotates the session id
	 * and replaces the CSRF token after a successful login.
	 *
	 * @param csrfTokenRepository the CSRF token repository
	 * @return the session authentication strategy
	 */
	@Bean
	public SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository)
	{
		return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),
		                                                          new CsrfAuthenticationStrategy(csrfTokenRepository)));
	}
}
