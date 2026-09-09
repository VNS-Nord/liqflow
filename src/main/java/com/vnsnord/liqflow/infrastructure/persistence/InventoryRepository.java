package com.vnsnord.liqflow.infrastructure.persistence;

import com.vnsnord.liqflow.domain.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Inventory} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD and pagination
 * operations, plus the lookups below that target inventory records by product,
 * location, or the combination of both.</p>
 *
 * @see Inventory
 */
public interface InventoryRepository extends JpaRepository<Inventory, UUID>
{
    /**
     * Finds the inventory record for a given product at a given location.
     *
     * @param productId  the product identifier
     * @param locationId the location identifier
     * @return the matching inventory record, or an empty {@link Optional} if none exists
     */
    Optional<Inventory> findByProductIdAndLocationId(UUID productId, UUID locationId);

    /**
     * Checks whether an inventory record exists for a given product at a given location.
     *
     * @param productId  the product identifier
     * @param locationId the location identifier
     * @return true if the inventory record exists, false otherwise
     */
    boolean existsByProductIdAndLocationId(UUID productId, UUID locationId);

    /**
     * Returns a page of inventory records stored at the given location.
     *
     * @param locationId the location identifier
     * @param pageable   pagination and sorting information
     * @return a page of inventory records for the location
     */
    Page<Inventory> findByLocationId(UUID locationId, Pageable pageable);
}
