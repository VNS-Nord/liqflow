package com.vnsnord.liqflow.identity.user;

import java.util.UUID;

/**
 * Response payload describing a user.
 *
 * @param id          the user identifier
 * @param username    the username
 * @param email       the email address
 * @param displayName the display name
 */
public record UserResponse(UUID id, String username, String email, String displayName)
{
}
