package com.vnsnord.liqflow.identity.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new user.
 *
 * @param username    the unique username, at most 100 characters
 * @param email       the unique, valid email address, at most 254 characters
 * @param displayName the display name, at most 100 characters
 * @param password    the raw password, between 15 and 128 characters
 */
public record CreateUserRequest(@NotBlank @Size(max = 100) String username,
								@NotBlank @Email @Size(max = 254) String email,
								@NotBlank @Size(max = 100) String displayName,
								@NotBlank @Size(min = 15, max = 128) String password)
{
}
