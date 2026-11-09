package com.vnsnord.liqflow.identity.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Permission} entities.
 *
 * @see Permission
 */
public interface PermissionRepository extends JpaRepository<Permission, UUID>
{
	/**
	 * Finds a permission by its unique code.
	 *
	 * @param code the permission code
	 * @return the matching permission, or an empty {@link Optional} if none exists
	 */
	Optional<Permission> findByCode(String code);
}
