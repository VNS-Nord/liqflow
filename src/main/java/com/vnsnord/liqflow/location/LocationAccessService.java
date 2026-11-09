package com.vnsnord.liqflow.location;

import com.vnsnord.liqflow.identity.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Enforces that the currently authenticated user is allowed to access a given
 * location.
 *
 * <p>Access is granted when the user has been explicitly assigned to the
 * location; anonymous or unauthenticated callers are always denied.</p>
 */
@Service
public class LocationAccessService
{
	private final UserRepository userRepository;

	/**
	 * Creates a new location access service backed by the given repository.
	 *
	 * @param userRepository the user repository
	 */
	public LocationAccessService(UserRepository userRepository)
	{
		this.userRepository = userRepository;
	}

	/**
	 * Requires that the current user has access to the given location.
	 *
	 * @param locationId the location identifier
	 * @throws AccessDeniedException if there is no authenticated user or the user
	 *                               is not assigned to the location
	 */
	@Transactional(readOnly = true)
	public void requireAccess(UUID locationId)
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || ! authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken)
		{
			throw new AccessDeniedException("Access denied");
		}
		boolean hasAccess = userRepository.existsByUsernameAndLocations_Id(authentication.getName(), locationId);
		if (! hasAccess)
		{
			throw new AccessDeniedException("Access denied");
		}
	}
}
