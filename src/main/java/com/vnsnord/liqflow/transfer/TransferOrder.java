package com.vnsnord.liqflow.transfer;

import com.vnsnord.liqflow.location.Location;
import com.vnsnord.liqflow.product.Product;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

/**
 * Represents an order to transfer products between two locations.
 *
 * <p>A transfer order progresses through the following lifecycle states:
 * {@code DRAFT → SUBMITTED → IN_TRANSIT → COMPLETED}.
 * It can be cancelled from any state except {@code COMPLETED}.</p>
 *
 * <p>Items ({@link TransferOrderItem}) can only be added or removed while the
 * order is in {@code DRAFT} status. Submitting an order with no items is not
 * allowed.</p>
 *
 * @see TransferOrderItem
 * @see TransferOrderStatus
 */
@Entity
@Table(
        name = "transfer_orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_transfer_order_number",
                columnNames = {"order_number"}
        )
)
public class TransferOrder
{
    @OneToMany(mappedBy = "transferOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<TransferOrderItem> items = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 60)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_location_id", nullable = false)
    private Location sourceLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_location_id", nullable = false)
    private Location targetLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferOrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected TransferOrder()
    {
    }

    /**
     * Creates a new transfer order in {@code DRAFT} status.
     *
     * @param orderNumber    unique business identifier for this order (must not be blank)
     * @param sourceLocation the location from which products will be transferred (must not be null)
     * @param targetLocation the location to which products will be transferred (must not be null)
     * @throws IllegalArgumentException if {@code orderNumber} is null or blank, or if both locations are the same
     * @throws NullPointerException     if {@code sourceLocation} or {@code targetLocation} is null
     */
    public TransferOrder(String orderNumber, Location sourceLocation, Location targetLocation)
    {
        if (orderNumber == null || orderNumber.isBlank())
        {
            throw new IllegalArgumentException("Order number cannot be null or empty");
        }
        this.sourceLocation = Objects.requireNonNull(sourceLocation, "Source location cannot be null");
        this.targetLocation = Objects.requireNonNull(targetLocation, "Target location cannot be null");
        if (isSameLocation(sourceLocation, targetLocation))
        {
            throw new IllegalArgumentException("Source and target locations cannot be the same");
        }
        this.orderNumber = orderNumber.trim();
        this.status = TransferOrderStatus.DRAFT;
    }

    /**
     * Determines whether two locations should be considered the same, either
     * because they are identical instances or because they share a persistent
     * identifier.
     *
     * @param first  the first location
     * @param second the second location
     * @return true if the locations are the same, false otherwise
     */
    private boolean isSameLocation(Location first, Location second)
    {
        if (first.equals(second))
        {
            return true;
        }
        return first.getId() != null && first.getId().equals(second.getId());
    }

    @PrePersist
    protected void onCreate()
    {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate()
    {
        this.updatedAt = Instant.now();
    }

    /**
     * Adds a product to this order. If the product already exists on the order,
     * the given quantity is added to the existing item instead of creating a new one.
     *
     * <p>A product is considered already present when it has the same
     * identifier as the item's product, or (for unsaved products) when it is
     * the same instance.</p>
     *
     * @param product  the product to add (must not be null)
     * @param quantity the number of units to transfer (must be &gt; 0)
     * @throws IllegalStateException    if the order is not in {@code DRAFT} status
     * @throws IllegalArgumentException if {@code quantity} is &le; 0
     */
    public void addItem(Product product, int quantity)
    {
        if (this.status != TransferOrderStatus.DRAFT)
        {
            throw new IllegalStateException("Items can only be added to a draft transfer order");
        }
        Objects.requireNonNull(product, "Product cannot be null");
        if (quantity <= 0)
        {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.items.stream()
                .filter(item -> isSameProduct(product, item.getProduct()))
                .findFirst()
                .ifPresentOrElse(
                        existingItem -> existingItem.addQuantity(quantity),
                        () -> this.items.add(new TransferOrderItem(this, product, quantity))
                );
    }

    /**
     * Determines whether the given products should be considered the same,
     * either because they share a persistent identifier or because they are
     * identical (unsaved) instances.
     *
     * @param first  the first product
     * @param second the second product
     * @return true if the products are the same, false otherwise
     */
    private boolean isSameProduct(Product first, Product second)
    {
        if (first.getId() != null && first.getId().equals(second.getId()))
        {
            return true;
        }
        return first.getId() == null && first.equals(second);
    }

    /**
     * Removes an item from this order.
     *
     * @param item the item to remove (must not be null)
     * @throws IllegalStateException if the order is not in {@code DRAFT} status
     * @throws NullPointerException  if {@code item} is null
     */
    public void removeItem(TransferOrderItem item)
    {
        if (this.status != TransferOrderStatus.DRAFT)
        {
            throw new IllegalStateException("Items can only be removed from a draft transfer order");
        }
        Objects.requireNonNull(item, "Item cannot be null");
        this.items.remove(item);
    }

    /**
     * Submits this order for processing, transitioning it from {@code DRAFT} to {@code SUBMITTED}.
     *
     * @throws IllegalStateException if the order is not in {@code DRAFT} status or has no items
     */
    public void submit()
    {
        if (this.status != TransferOrderStatus.DRAFT)
        {
            throw new IllegalStateException("Only draft transfer orders can be submitted");
        }
        if (this.items.isEmpty())
        {
            throw new IllegalStateException("Cannot submit an empty transfer order");
        }
        this.status = TransferOrderStatus.SUBMITTED;
    }

    /**
     * Marks this order as in transit, transitioning it from {@code SUBMITTED} to {@code IN_TRANSIT}.
     *
     * @throws IllegalStateException if the order is not in {@code SUBMITTED} status
     */
    public void markInTransit()
    {
        if (this.status != TransferOrderStatus.SUBMITTED)
        {
            throw new IllegalStateException("Only submitted transfer orders can be marked in transit");
        }
        this.status = TransferOrderStatus.IN_TRANSIT;
    }

    /**
     * Completes this order, transitioning it from {@code IN_TRANSIT} to {@code COMPLETED}.
     *
     * @throws IllegalStateException if the order is not in {@code IN_TRANSIT} status
     */
    public void complete()
    {
        if (this.status != TransferOrderStatus.IN_TRANSIT)
        {
            throw new IllegalStateException("Only in-transit transfer orders can be completed");
        }
        this.status = TransferOrderStatus.COMPLETED;
    }

    /**
     * Cancels this order. A completed order cannot be cancelled.
     * If the order is already cancelled, this method is a no-op.
     *
     * @throws IllegalStateException if the order is in {@code COMPLETED} status
     */
    public void cancel()
    {
        if (this.status == TransferOrderStatus.COMPLETED)
        {
            throw new IllegalStateException("Cannot cancel a completed transfer order");
        }
        if (this.status == TransferOrderStatus.CANCELLED)
        {
            return;
        }
        this.status = TransferOrderStatus.CANCELLED;
    }

    /**
     * @return the order identifier
     */
    public UUID getId()
    {
        return id;
    }

    /**
     * @return the unique business order number
     */
    public String getOrderNumber()
    {
        return orderNumber;
    }

    /**
     * @return the source location of the transfer
     */
    public Location getSourceLocation()
    {
        return sourceLocation;
    }

    /**
     * @return the target location of the transfer
     */
    public Location getTargetLocation()
    {
        return targetLocation;
    }

    /**
     * @return the current lifecycle status
     */
    public TransferOrderStatus getStatus()
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
     * @return the timestamp of the last update
     */
    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    /**
     * @return the line items of this order, as an unmodifiable list
     */
    public List<TransferOrderItem> getItems()
    {
        return Collections.unmodifiableList(items);
    }

    /**
     * @return the optimistic-locking version
     */
    public Long getVersion()
    {
        return version;
    }
}
