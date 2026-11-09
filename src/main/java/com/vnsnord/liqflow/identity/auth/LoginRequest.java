package com.vnsnord.liqflow.identity.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload carrying the credentials used to authenticate a user.
 *
 * @param username the username, at most 100 characters
 * @param password the raw password, at most 128 characters
 */
public record LoginRequest(@NotBlank @Size(max = 100) String username, @NotBlank @Size(max = 128) String password)
{
}
