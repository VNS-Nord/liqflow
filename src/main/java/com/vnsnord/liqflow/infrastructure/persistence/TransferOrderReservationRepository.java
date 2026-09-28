package com.vnsnord.liqflow.infrastructure.persistence;

import com.vnsnord.liqflow.domain.entity.TransferOrderReservation;
import com.vnsnord.liqflow.domain.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TransferOrderReservation} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations, plus the
 * order-scoped lookups used while completing or cancelling a transfer order and
 * the existence checks that guard product and location deletion.</p>
 *
 * @see TransferOrderReservation
 */
public interface TransferOrderReservationRepository extends JpaRepository<TransferOrderReservation, UUID>
{
    /**
     * Finds all reservations belonging to a transfer order, ordered by
     * reservation identifier.
     *
     * @param transferOrderId the transfer order identifier
     * @return the order's reservations, empty if it has none
     */
    @EntityGraph(attributePaths = {"item", "product", "location"})
    @Query("SELECT r FROM TransferOrderReservation r WHERE r.transferOrder.id = :transferOrderId ORDER BY r.id")
    List<TransferOrderReservation> findByTransferOrderId(@Param("transferOrderId") UUID transferOrderId);

    /**
     * Finds the reservations of a transfer order that are in the given status,
     * ordered by reservation identifier so that callers always process them in
     * the same sequence.
     *
     * @param transferOrderId the transfer order identifier
     * @param status          the status to filter on
     * @return the matching reservations, empty if there are none
     */
    @EntityGraph(attributePaths = {"item", "product", "location"})
    @Query("SELECT r FROM TransferOrderReservation r "
            + "WHERE r.transferOrder.id = :transferOrderId AND r.status = :status ORDER BY r.id")
    List<TransferOrderReservation> findByTransferOrderIdAndStatus(@Param("transferOrderId") UUID transferOrderId,
                                                                    @Param("status") ReservationStatus status);

    /**
     * Finds the reservations of a transfer order, acquiring a pessimistic write
     * lock on every row for the duration of the transaction.
     *
     * <p>Used when completing or cancelling an order so that the held
     * reservations cannot be settled twice by concurrent transactions. Rows are
     * returned in reservation-identifier order, which gives every caller the
     * same lock-acquisition sequence and therefore rules out deadlocks between
     * them.</p>
     *
     * @param transferOrderId the transfer order identifier
     * @return the order's locked reservations, empty if it has none
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"item", "product", "location"})
    @Query("SELECT r FROM TransferOrderReservation r WHERE r.transferOrder.id = :transferOrderId ORDER BY r.id")
    List<TransferOrderReservation> findByTransferOrderIdForUpdate(@Param("transferOrderId") UUID transferOrderId);

    /**
     * Finds the reservation created for a given line item.
     *
     * @param itemId the transfer order item identifier
     * @return the matching reservation, or an empty {@link Optional} if none exists
     */
    @EntityGraph(attributePaths = {"item", "product", "location"})
    @Query("SELECT r FROM TransferOrderReservation r WHERE r.item.id = :itemId")
    Optional<TransferOrderReservation> findByItemId(@Param("itemId") UUID itemId);

    /**
     * Checks whether any reservation references the given product.
     *
     * @param productId the product identifier
     * @return true if at least one reservation references the product, false otherwise
     */
    boolean existsByProductId(UUID productId);

    /**
     * Checks whether any reservation references the given location.
     *
     * @param locationId the location identifier
     * @return true if at least one reservation references the location, false otherwise
     */
    boolean existsByLocationId(UUID locationId);
}
