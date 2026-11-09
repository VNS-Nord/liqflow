package com.vnsnord.liqflow.identity.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * @see User
 */
public interface UserRepository extends JpaRepository<User, UUID>
{
	/**
	 * Finds a user by username.
	 *
	 * @param username the username to look up
	 * @return the matching user, or an empty {@link Optional} if none exists
	 */
	Optional<User> findByUsername(String username);

	/**
	 * Finds a user by username, eagerly fetching its roles, permissions, and
	 * accessible locations.
	 *
	 * @param username the username to look up
	 * @return the matching user with access data, or an empty {@link Optional} if none exists
	 */
	@EntityGraph(attributePaths = {"roles.permissions", "locations"})
	Optional<User> findWithAccessByUsername(String username);

	/**
	 * Checks whether a user exists with the given username.
	 *
	 * @param username the username to check
	 * @return {@code true} if such a user exists
	 */
	boolean existsByUsername(String username);

	/**
	 * Checks whether a user exists with the given email address.
	 *
	 * @param email the email address to check
	 * @return {@code true} if such a user exists
	 */
	boolean existsByEmail(String email);

	/**
	 * Checks whether a user with the given username has access to the given
	 * location.
	 *
	 * @param username   the username to check
	 * @param locationId the location identifier
	 * @return {@code true} if the user exists and is granted access to the location
	 */
	boolean existsByUsernameAndLocations_Id(String username, UUID locationId);
}
