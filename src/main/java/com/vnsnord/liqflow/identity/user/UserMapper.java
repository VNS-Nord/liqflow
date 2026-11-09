package com.vnsnord.liqflow.identity.user;

import org.springframework.stereotype.Component;

/**
 * Maps between {@link User} entities and their request/response DTOs.
 */
@Component
public class UserMapper
{
	/**
	 * Converts a user entity to its response DTO.
	 *
	 * @param user the user entity
	 * @return the corresponding response DTO
	 */
	public UserResponse toResponse(User user)
	{
		return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName());
	}

	/**
	 * Creates a user entity from a creation request and an encoded password hash.
	 *
	 * @param request      the user creation request
	 * @param passwordHash the encoded password hash
	 * @return the new user entity
	 */
	public User toEntity(CreateUserRequest request, String passwordHash)
	{
		return new User(request.username(), request.email(), request.displayName(), passwordHash);
	}
}
