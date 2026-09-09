package com.vnsnord.liqflow.infrastructure.persistence;

import com.vnsnord.liqflow.domain.entity.TransferOrder;
import com.vnsnord.liqflow.domain.enums.TransferOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TransferOrder} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD and pagination
 * operations, plus the order-number, status, and location-based queries
 * declared below. {@link #findWithDetailsById(UUID)} eagerly fetches the order
 * items and both locations to avoid lazy-loading issues outside a transaction.</p>
 *
 * @see TransferOrder
 * @see TransferOrderStatus
 */
public interface TransferOrderRepository extends JpaRepository<TransferOrder, UUID>
{
    /**
     * Finds a transfer order by its unique order number.
     *
     * @param orderNumber the order number to search for
     * @return the matching order, or an empty {@link Optional} if none exists
     */
    Optional<TransferOrder> findByOrderNumber(String orderNumber);

    /**
     * Checks whether a transfer order with the given order number exists.
     *
     * @param orderNumber the order number to check
     * @return true if an order with the number exists, false otherwise
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Returns a page of transfer orders filtered by status.
     *
     * @param status   the status to filter on
     * @param pageable pagination and sorting information
     * @return a page of orders with the given status
     */
    Page<TransferOrder> findByStatus(TransferOrderStatus status, Pageable pageable);

    /**
     * Returns a page of transfer orders in which the given location is either
     * the source or the target.
     *
     * @param sourceId the source location identifier
     * @param targetId the target location identifier
     * @param pageable pagination and sorting information
     * @return a page of orders involving the location
     */
    Page<TransferOrder> findBySourceLocationIdOrTargetLocationId(UUID sourceId, UUID targetId, Pageable pageable);

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
     * Checks whether any transfer order involves the given location and is in
     * one of the given statuses.
     *
     * @param locationId the location identifier
     * @param statuses   the statuses to check for
     * @return true if at least one matching order exists, false otherwise
     */
    @Query("SELECT COUNT(t) > 0 FROM TransferOrder t WHERE (t.sourceLocation.id = :locationId OR t.targetLocation.id = :locationId) AND t.status IN :statuses")
    boolean existsByLocationIdAndStatusIn(@Param("locationId") UUID locationId, @Param("statuses") Collection<TransferOrderStatus> statuses);


}
