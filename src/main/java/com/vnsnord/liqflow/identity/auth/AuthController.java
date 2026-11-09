package com.vnsnord.liqflow.identity.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoints for authentication under {@code /api/v1/auth}.
 *
 * <p>Handles session-based login, exposes the CSRF token required by
 * state-changing requests, and reports the currently authenticated user.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController
{
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

	/**
	 * Creates a new authentication controller with the given dependencies.
	 *
	 * @param authenticationManager          the authentication manager used to verify credentials
	 * @param securityContextRepository      the repository used to persist the security context
	 * @param sessionAuthenticationStrategy  the strategy applied on successful authentication
	 */
	public AuthController(AuthenticationManager authenticationManager,
						  SecurityContextRepository securityContextRepository,
						  SessionAuthenticationStrategy sessionAuthenticationStrategy)
	{
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
	}

	/**
	 * Authenticates a user and establishes an HTTP session for them.
	 *
	 * @param loginRequest the username and password credentials
	 * @param request      the current HTTP request
	 * @param response     the current HTTP response
	 * @return a 204 response on success
	 */
	@PostMapping("/login")
	public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request,
									  HttpServletResponse response)
	{
		Authentication authManager = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
				loginRequest.username(),
				loginRequest.password()));
		sessionAuthenticationStrategy.onAuthentication(authManager, request, response);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authManager);
		SecurityContextHolder.setContext(context);

		securityContextRepository.saveContext(context, request, response);

		return ResponseEntity.noContent()
							 .build();
	}

	/**
	 * Returns the current CSRF token so clients can include it in subsequent
	 * state-changing requests.
	 *
	 * @param csrfToken the resolved CSRF token
	 * @return the CSRF token
	 */
	@GetMapping("/csrf")
	public CsrfToken csrf(CsrfToken csrfToken)
	{
		return csrfToken;
	}

	/**
	 * Returns the username of the currently authenticated user.
	 *
	 * @param authentication the current authentication
	 * @return a map containing the authenticated username
	 */
	@GetMapping("/me")
	public Map<String, String> me(Authentication authentication)
	{
		return Map.of("username", authentication.getName());
	}

}
