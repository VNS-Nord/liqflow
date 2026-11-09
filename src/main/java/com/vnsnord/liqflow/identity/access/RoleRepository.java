package com.vnsnord.liqflow.identity.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Role} entities.
 *
 * @see Role
 */
public interface RoleRepository extends JpaRepository<Role, UUID>
{
	/**
	 * Finds a role by its unique code.
	 *
	 * @param code the role code
	 * @return the matching role, or an empty {@link Optional} if none exists
	 */
	Optional<Role> findByCode(String code);
}
