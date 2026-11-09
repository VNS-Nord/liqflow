package com.vnsnord.liqflow.identity.access;

import jakarta.persistence.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a named role that groups a set of {@link Permission permissions}.
 *
 * <p>Roles are assigned to users and are identified by a unique {@code code}
 * (for example {@code ADMIN}). The permissions granted to a role become the
 * authorities of any user that holds it.</p>
 *
 * @see Permission
 */
@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(name = "uk_role_code", columnNames = "code"))
public class Role
{
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", length = 500)
	private String description;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
	private Set<Permission> permissions = new HashSet<>();

	/**
	 * Creates an empty role instance for use by the persistence provider.
	 */
	protected Role()
	{
	}

	/**
	 * Returns the role identifier.
	 *
	 * @return the role identifier
	 */
	public UUID getId()
	{
		return id;
	}

	/**
	 * Returns the unique role code.
	 *
	 * @return the role code
	 */
	public String getCode()
	{
		return code;
	}

	/**
	 * Returns the human-readable role name.
	 *
	 * @return the role name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the human-readable role description.
	 *
	 * @return the role description, or {@code null} if none is set
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * Returns the permissions granted to this role as an unmodifiable set.
	 *
	 * @return the role's permissions
	 */
	public Set<Permission> getPermissions()
	{
		return Collections.unmodifiableSet(permissions);
	}
}