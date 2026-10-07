package com.vnsnord.liqflow.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
     * Finds the inventory record for a given product at a given location,
     * acquiring a pessimistic write lock for the duration of the transaction.
     *
     * <p>Used by stock-movement operations (such as completing a transfer
     * order) to serialise concurrent updates of the same record and prevent
     * lost updates.</p>
     *
     * @param productId  the product identifier
     * @param locationId the location identifier
     * @return the matching inventory record, or an empty {@link Optional} if none exists
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.location.id = :locationId")
    Optional<Inventory> findByProductIdAndLocationIdForUpdate(@Param("productId") UUID productId,
                                                              @Param("locationId") UUID locationId);

    /**
     * Checks whether an inventory record exists for a given product at a given location.
     *
     * @param productId  the product identifier
     * @param locationId the location identifier
     * @return true if the inventory record exists, false otherwise
     */
    boolean existsByProductIdAndLocationId(UUID productId, UUID locationId);

    /**
     * Returns a page of inventory records stored at the given location, eagerly
     * fetching the holding location and the stocked product.
     *
     * @param locationId the location identifier
     * @param pageable   pagination and sorting information
     * @return a page of inventory records for the location
     */
    @EntityGraph(attributePaths = {"location", "product"})
    Page<Inventory> findByLocationId(UUID locationId, Pageable pageable);

    /**
     * Checks whether any inventory record exists for the given product.
     *
     * @param productId the product identifier
     * @return true if at least one record exists, false otherwise
     */
    boolean existsByProductId(UUID productId);

    /**
     * Checks whether any inventory record exists at the given location.
     *
     * @param locationId the location identifier
     * @return true if at least one record exists, false otherwise
     */
    boolean existsByLocationId(UUID locationId);

    /**
     * Returns all inventory records, eagerly fetching the holding location and
     * the stocked product to avoid a query per row.
     *
     * @return the list of inventory records
     */
    @EntityGraph(attributePaths = {"location", "product"})
    @Query("SELECT i FROM Inventory i")
    List<Inventory> findAllWithDetails();

    /**
     * Returns all inventory records whose available quantity (physical minus
     * reserved) is at or below the configured minimum threshold, eagerly
     * fetching the holding location and the stocked product.
     *
     * @return the list of low-stock inventory records
     */
    @EntityGraph(attributePaths = {"location", "product"})
    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= i.minThreshold")
    List<Inventory> findLowStock();
}
