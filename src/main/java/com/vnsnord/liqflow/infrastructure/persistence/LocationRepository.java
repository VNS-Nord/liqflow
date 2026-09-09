package com.vnsnord.liqflow.infrastructure.persistence;

import com.vnsnord.liqflow.domain.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Location} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD and pagination
 * operations, plus the {@code code}-based lookups declared below.</p>
 *
 * @see Location
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, UUID>
{
    /**
     * Finds a location by its unique code.
     *
     * @param code the location code to search for
     * @return the matching location, or an empty {@link Optional} if none exists
     */
    Optional<Location> findByCode(String code);

    /**
     * Checks whether a location with the given code exists.
     *
     * @param code the location code to check
     * @return true if a location with the code exists, false otherwise
     */
    boolean existsByCode(String code);
}
