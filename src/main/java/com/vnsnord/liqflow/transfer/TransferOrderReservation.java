package com.vnsnord.liqflow.transfer;

import com.vnsnord.liqflow.product.Product;
import com.vnsnord.liqflow.inventory.Inventory;
import com.vnsnord.liqflow.location.Location;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records that a {@link TransferOrderItem} has set aside a quantity of stock at
 * a source {@link Location}, so that completing the transfer never consumes
 * units that were reserved for a different order.
 *
 * <p>A reservation is created in {@link ReservationStatus#HELD} when its order
 * is submitted and settles exactly once, either as
 * {@link ReservationStatus#CONSUMED} on completion or as
 * {@link ReservationStatus#RELEASED} on cancellation. While it is held, the
 * corresponding {@code reservedQuantity} on the
 * {@link Inventory#getReservedQuantity() source inventory record} counts these
 * units, which keeps them out of other orders' reach.</p>
 *
 * @see Inventory
 * @see TransferOrder
 */
@Entity
@Table(
        name = "transfer_order_reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_item",
                columnNames = {"transfer_order_item_id"}
        )
)
public class TransferOrderReservation
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_order_id", nullable = false)
    private TransferOrder transferOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_order_item_id", nullable = false)
    private TransferOrderItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected TransferOrderReservation()
    {
    }

    /**
     * Creates a new reservation in {@link ReservationStatus#HELD} status for a
     * single line item of a transfer order.
     *
     * @param transferOrder the owning transfer order (must not be null)
     * @param item          the line item being reserved (must not be null)
     * @param product       the product being reserved (must not be null)
     * @param location      the location the stock is reserved at (must not be null)
     * @param quantity      the number of units to reserve (must be &gt; 0)
     * @throws IllegalArgumentException if {@code quantity} is &le; 0
     * @throws NullPointerException     if any reference argument is null
     */
    public TransferOrderReservation(TransferOrder transferOrder,
                                    TransferOrderItem item,
                                    Product product,
                                    Location location,
                                    int quantity)
    {
        if (quantity <= 0)
        {
            throw new IllegalArgumentException("Reservation quantity must be greater than zero");
        }
        this.transferOrder = Objects.requireNonNull(transferOrder, "Transfer order cannot be null");
        this.item = Objects.requireNonNull(item, "Transfer order item cannot be null");
        this.product = Objects.requireNonNull(product, "Product cannot be null");
        this.location = Objects.requireNonNull(location, "Location cannot be null");
        this.quantity = quantity;
        this.status = ReservationStatus.HELD;
    }

    @PrePersist
    protected void onCreate()
    {
        this.createdAt = Instant.now();
    }

    /**
     * Marks this reservation as consumed, recording that the stock physically
     * left the source location. Only a held reservation can be consumed.
     *
     * @throws IllegalStateException if the reservation is not {@link ReservationStatus#HELD}
     */
    public void markConsumed()
    {
        if (this.status != ReservationStatus.HELD)
        {
            throw new IllegalStateException("Only a held reservation can be consumed");
        }
        this.status = ReservationStatus.CONSUMED;
    }

    /**
     * Marks this reservation as released, returning the units to the available
     * quantity of the source location. Only a held reservation can be released.
     *
     * @throws IllegalStateException if the reservation is not {@link ReservationStatus#HELD}
     */
    public void markReleased()
    {
        if (this.status != ReservationStatus.HELD)
        {
            throw new IllegalStateException("Only a held reservation can be released");
        }
        this.status = ReservationStatus.RELEASED;
    }

    /**
     * @return the reservation identifier
     */
    public UUID getId()
    {
        return id;
    }

    /**
     * @return the owning transfer order
     */
    public TransferOrder getTransferOrder()
    {
        return transferOrder;
    }

    /**
     * @return the line item this reservation was created for
     */
    public TransferOrderItem getItem()
    {
        return item;
    }

    /**
     * @return the reserved product
     */
    public Product getProduct()
    {
        return product;
    }

    /**
     * @return the location the stock is reserved at
     */
    public Location getLocation()
    {
        return location;
    }

    /**
     * @return the number of units held by this reservation
     */
    public int getQuantity()
    {
        return quantity;
    }

    /**
     * @return the current lifecycle status
     */
    public ReservationStatus getStatus()
    {
        return status;
    }

    /**
     * @return the creation timestamp
     */
    public Instant getCreatedAt()
    {
        return createdAt;
    }

    /**
     * @return the optimistic-locking version
     */
    public Long getVersion()
    {
        return version;
    }
}
