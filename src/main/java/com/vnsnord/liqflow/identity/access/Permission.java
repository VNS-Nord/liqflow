package com.vnsnord.liqflow.identity.access;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Represents a single permission that can be granted to a {@link Role}.
 *
 * <p>Permissions are identified by a stable, unique {@code code} (for example
 * {@code USER_MANAGE}) that is used as the Spring Security authority when
 * authorizing requests.</p>
 *
 * @see Role
 */
@Entity
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(name = "uk_permission_code", columnNames = "code"))
public class Permission
{
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "code", nullable = false, length = 100)
	private String code;

	@Column(name = "description", length = 500)
	private String description;

	/**
	 * Creates an empty permission instance for use by the persistence provider.
	 */
	protected Permission()
	{
	}

	/**
	 * Returns the permission identifier.
	 *
	 * @return the permission identifier
	 */
	public UUID getId()
	{
		return id;
	}

	/**
	 * Returns the unique permission code.
	 *
	 * @return the permission code
	 */
	public String getCode()
	{
		return code;
	}

	/**
	 * Returns the human-readable description of the permission.
	 *
	 * @return the permission description, or {@code null} if none is set
	 */
	public String getDescription()
	{
		return description;
	}
}