package com.vnsnord.liqflow.identity.user;

import com.vnsnord.liqflow.identity.access.Role;
import com.vnsnord.liqflow.location.Location;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

/**
 * Represents an application user, including its credentials, roles, and
 * location access grants.
 *
 * <p>Usernames are normalized to trimmed lower case. The class tracks a
 * monotonically increasing {@code securityVersion} that is bumped whenever a
 * security-relevant property changes (password, enabled state, roles, or
 * locations), allowing sessions to be invalidated when needed.</p>
 *
 * @see Role
 * @see Location
 */
@Entity
@Table(name = "users")
public class User
{
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "username", nullable = false, unique = true, length = 100)
	private String username;

	@Column(name = "email", nullable = false, unique = true, length = 254)
	private String email;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "enabled", nullable = false)
	private boolean enabled = true;

	@Column(name = "security_version", nullable = false)
	private long securityVersion = 0L;

	@Column(name = "password_changed_at", nullable = false)
	private Instant passwordChangedAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_locations", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "location_id"))
	private Set<Location> locations = new HashSet<>();

	/**
	 * Creates an empty user instance for use by the persistence provider.
	 */
	protected User()
	{
	}

	/**
	 * Creates a new enabled user with the given profile data.
	 *
	 * @param username     the username; it is trimmed and lower-cased
	 * @param email        the email address
	 * @param displayName  the display name
	 * @param passwordHash the already-encoded password hash
	 * @throws NullPointerException     if any argument is {@code null}
	 * @throws IllegalArgumentException if a value is blank or exceeds its maximum length
	 */
	public User(String username, String email, String displayName, String passwordHash)
	{
		String normalizedUsername = Objects.requireNonNull(username, "username must not be null").trim()
		                                   .toLowerCase(Locale.ROOT);

		this.username = requireText(normalizedUsername, "username", 100);
		this.email = requireText(email, "email", 254).trim();
		this.displayName = requireText(displayName, "displayName", 100).trim();
		this.passwordHash = requireText(passwordHash, "passwordHash", 255);
		this.passwordChangedAt = Instant.now();
	}

	private static String requireText(String value, String fieldName, int maxLength)
	{
		Objects.requireNonNull(value, fieldName + " must not be null");
		if (value.isBlank())
		{
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		if (value.length() > maxLength)
		{
			throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return value;
	}

	private static <T> Set<T> validatedCopy(Set<T> values, String fieldName)
	{
		Objects.requireNonNull(values, fieldName + " must not be null");
		Set<T> copy = new HashSet<>();
		for (T value : values)
		{
			copy.add(Objects.requireNonNull(value, fieldName + " must not contain null elements"));
		}
		return copy;
	}

	/**
	 * Returns the user identifier.
	 *
	 * @return the user identifier
	 */
	public UUID getId()
	{
		return id;
	}

	/**
	 * Returns the normalized username.
	 *
	 * @return the username
	 */
	public String getUsername()
	{
		return username;
	}

	/**
	 * Returns the user's email address.
	 *
	 * @return the email address
	 */
	public String getEmail()
	{
		return email;
	}

	/**
	 * Returns the user's display name.
	 *
	 * @return the display name
	 */
	public String getDisplayName()
	{
		return displayName;
	}

	/**
	 * Returns the encoded password hash.
	 *
	 * @return the password hash
	 */
	public String getPasswordHash()
	{
		return passwordHash;
	}

	/**
	 * Indicates whether the user account is enabled.
	 *
	 * @return {@code true} if the account is enabled
	 */
	public boolean isEnabled()
	{
		return enabled;
	}

	/**
	 * Returns the current security version.
	 *
	 * @return the security version
	 */
	public long getSecurityVersion()
	{
		return securityVersion;
	}

	/**
	 * Returns the instant at which the password last changed.
	 *
	 * @return the password change timestamp
	 */
	public Instant getPasswordChangedAt()
	{
		return passwordChangedAt;
	}

	/**
	 * Returns the instant of the user's last successful login.
	 *
	 * @return the last login timestamp, or {@code null} if the user never logged in
	 */
	public Instant getLastLoginAt()
	{
		return lastLoginAt;
	}

	/**
	 * Returns the instant at which the user was created.
	 *
	 * @return the creation timestamp
	 */
	public Instant getCreatedAt()
	{
		return createdAt;
	}

	/**
	 * Returns the instant at which the user was last updated.
	 *
	 * @return the last-update timestamp
	 */
	public Instant getUpdatedAt()
	{
		return updatedAt;
	}

	/**
	 * Returns the optimistic-locking version.
	 *
	 * @return the version
	 */
	public Long getVersion()
	{
		return version;
	}

	/**
	 * Returns the roles assigned to the user as an unmodifiable set.
	 *
	 * @return the user's roles
	 */
	public Set<Role> getRoles()
	{
		return Collections.unmodifiableSet(roles);
	}

	/**
	 * Returns the locations the user may access as an unmodifiable set.
	 *
	 * @return the user's accessible locations
	 */
	public Set<Location> getLocations()
	{
		return Collections.unmodifiableSet(locations);
	}

	/**
	 * Changes the user's display name.
	 *
	 * @param newDisplayName the new display name
	 * @throws IllegalArgumentException if the value is blank or exceeds 100 characters
	 */
	public void changeDisplayName(String newDisplayName)
	{
		displayName = requireText(newDisplayName, "displayName", 100).trim();
	}

	/**
	 * Changes the user's password hash, records the change time, and bumps the
	 * security version.
	 *
	 * @param newPasswordHash the new encoded password hash
	 * @throws IllegalArgumentException if the value is blank or exceeds 255 characters
	 */
	public void changePassword(String newPasswordHash)
	{
		passwordHash = requireText(newPasswordHash, "passwordHash", 255);
		passwordChangedAt = Instant.now();
		incrementSecurityVersion();
	}

	/**
	 * Enables the account, bumping the security version if it was disabled.
	 */
	public void enable()
	{
		if (! enabled)
		{
			enabled = true;
			incrementSecurityVersion();
		}
	}

	/**
	 * Disables the account, bumping the security version if it was enabled.
	 */
	public void disable()
	{
		if (enabled)
		{
			enabled = false;
			incrementSecurityVersion();
		}
	}

	/**
	 * Records the current instant as the user's last successful login.
	 */
	public void recordLogin()
	{
		lastLoginAt = Instant.now();
	}

	/**
	 * Replaces the user's roles and bumps the security version.
	 *
	 * @param newRoles the roles to assign
	 * @throws NullPointerException if the set or any element is {@code null}
	 */
	public void replaceRoles(Set<Role> newRoles)
	{
		Set<Role> replacement = validatedCopy(newRoles, "roles");

		roles.clear();
		roles.addAll(replacement);

		incrementSecurityVersion();
	}

	/**
	 * Replaces the user's accessible locations and bumps the security version.
	 *
	 * @param newLocations the locations to grant access to
	 * @throws NullPointerException if the set or any element is {@code null}
	 */
	public void replaceLocations(Set<Location> newLocations)
	{
		Set<Location> replacement = validatedCopy(newLocations, "locations");

		locations.clear();
		locations.addAll(replacement);

		incrementSecurityVersion();
	}

	/**
	 * Increments the security version to invalidate previously issued sessions
	 * or tokens.
	 */
	public void incrementSecurityVersion()
	{
		securityVersion++;
	}

	@PrePersist
	private void onCreate()
	{
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	private void onUpdate()
	{
		updatedAt = Instant.now();
	}
}