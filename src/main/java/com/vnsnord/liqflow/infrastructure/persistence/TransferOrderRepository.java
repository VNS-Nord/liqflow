package com.vnsnord.liqflow.infrastructure.persistence;

import com.vnsnord.liqflow.domain.entity.TransferOrder;
import com.vnsnord.liqflow.domain.enums.TransferOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TransferOrder} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD and pagination
 * operations, plus the order-number and location-based queries declared below.
 * {@link #findWithDetailsById(UUID)} eagerly fetches the order items and both
 * locations, and {@link #findAllWithLocations(Pageable)} eagerly fetches both
 * locations for list views, to avoid lazy-loading issues outside a
 * transaction.</p>
 *
 * @see TransferOrder
 * @see TransferOrderStatus
 */
public interface TransferOrderRepository extends JpaRepository<TransferOrder, UUID>
{
    /**
     * Checks whether a transfer order with the given order number exists.
     *
     * @param orderNumber the order number to check
     * @return true if an order with the number exists, false otherwise
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Returns a page of all transfer orders, eagerly fetching both the source
     * and target locations to avoid a query per row.
     *
     * @param pageable pagination and sorting information
     * @return a page of orders with their locations loaded
     */
    @EntityGraph(attributePaths = {"sourceLocation", "targetLocation"})
    @Query("SELECT t FROM TransferOrder t")
    Page<TransferOrder> findAllWithLocations(Pageable pageable);

    /**
     * Finds a transfer order by id, eagerly loading its items, source location,
     * and target location.
     *
     * @param id the order identifier
     * @return the matching order with its details loaded, or an empty {@link Optional} if none exists
     */
    @EntityGraph(attributePaths = {"items", "sourceLocation", "targetLocation"})
    @Query("SELECT t FROM TransferOrder t WHERE t.id = :id")
    Optional<TransferOrder> findWithDetailsById(@Param("id") UUID id);

    /**
     * Checks whether any transfer order involves the given location as its
     * source or target.
     *
     * @param locationId the location identifier
     * @return true if at least one matching order exists, false otherwise
     */
    @Query("SELECT COUNT(t) > 0 FROM TransferOrder t WHERE t.sourceLocation.id = :locationId OR t.targetLocation.id = :locationId")
    boolean existsByLocationId(@Param("locationId") UUID locationId);

    /**
     * Checks whether any transfer order item references the given product.
     *
     * @param productId the product identifier
     * @return true if at least one matching item exists, false otherwise
     */
    @Query("SELECT COUNT(i) > 0 FROM TransferOrderItem i WHERE i.product.id = :productId")
    boolean existsItemForProduct(@Param("productId") UUID productId);
}
