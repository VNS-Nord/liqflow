package com.vnsnord.liqflow.inventory;

import com.vnsnord.liqflow.location.Location;
import com.vnsnord.liqflow.product.Product;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents the stock of a {@link Product} held at a {@link Location}.
 *
 * <p>Each inventory record is unique for a given {@code location} and
 * {@code product} combination. It tracks the physical quantity on hand, the
 * quantity reserved for pending orders, and a minimum threshold used to signal
 * when stock is running low.</p>
 *
 * <p>The available quantity is computed as the physical quantity minus the
 * reserved quantity ({@link #getAvailableQuantity()}). Stock can be reserved,
 * released, consumed, deducted, or added through the domain methods on this
 * class, all of which enforce the required invariants.</p>
 *
 * @see Location
 * @see Product
 */
@Entity
@Table(
        name = "inventories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_location_product",
                columnNames = {"location_id", "product_id"}
        )
)
public class Inventory
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Column(name = "min_threshold", nullable = false)
    private int minThreshold = 0;

    @Version
    private Long version;

    protected Inventory()
    {
    }

    /**
     * Creates a new inventory record for the given location, product and initial stock.
     *
     * @param location     the location where the stock is held (must not be null)
     * @param product      the product being stocked (must not be null)
     * @param initialStock the initial physical quantity on hand (must be &ge; 0)
     * @param minThreshold the minimum quantity threshold (must be &ge; 0)
     * @throws IllegalArgumentException if {@code initialStock} or {@code minThreshold} is negative
     * @throws NullPointerException     if {@code location} or {@code product} is null
     */
    public Inventory(Location location, Product product, int initialStock, int minThreshold)
    {
        if (initialStock < 0)
        {
            throw new IllegalArgumentException("Initial stock cannot be negative");
        }
        if (minThreshold < 0)
        {
            throw new IllegalArgumentException("Minimum threshold cannot be negative");
        }
        this.location = Objects.requireNonNull(location, "Location cannot be null");
        this.product = Objects.requireNonNull(product, "Product cannot be null");
        this.quantity = initialStock;
        this.minThreshold = minThreshold;
        this.reservedQuantity = 0;
    }

    /**
     * Returns the quantity available for new reservations or fulfillment,
     * computed as the physical quantity minus the reserved quantity.
     *
     * @return the currently available quantity
     */
    public int getAvailableQuantity()
    {
        return this.quantity - this.reservedQuantity;
    }

    /**
     * Reserves a quantity of stock, increasing the reserved quantity.
     *
     * @param amount the number of units to reserve (must be &gt; 0)
     * @throws IllegalArgumentException if {@code amount} is &le; 0
     * @throws IllegalStateException    if there is insufficient available stock
     */
    public void reserveStock(int amount)
    {
        if (amount <= 0)
        {
            throw new IllegalArgumentException("Quantity to reserve must be greater than zero");
        }
        if (getAvailableQuantity() < amount)
        {
            throw new IllegalStateException("Insufficient available stock for reservation");
        }
        this.reservedQuantity += amount;
    }

    /**
     * Releases a previously reserved quantity of stock.
     *
     * @param amount the number of units to release (must be &gt; 0)
     * @throws IllegalArgumentException if {@code amount} is &le; 0
     * @throws IllegalStateException    if more than the currently reserved quantity is released
     */
    public void releaseReservedStock(int amount)
    {
        if (amount <= 0)
        {
            throw new IllegalArgumentException("Quantity to release must be greater than zero");
        }
        if (this.reservedQuantity < amount)
        {
            throw new IllegalStateException("Cannot release more than currently reserved stock");
        }
        this.reservedQuantity -= amount;
    }

    /**
     * Deducts a quantity from the physical stock on hand. The deduction may only
     * come out of the available quantity, so units reserved for a pending
     * transfer are never touched. This keeps {@code reservedQuantity} in
     * agreement with the reservation rows that back it; an operator who needs to
     * correct stock that is already committed must settle the order first.
     *
     * @param amount the number of units to deduct (must be &gt; 0)
     * @throws IllegalArgumentException if {@code amount} is &le; 0
     * @throws IllegalStateException    if {@code amount} exceeds the available quantity
     */
    public void deductStock(int amount)
    {
        if (amount <= 0)
        {
            throw new IllegalArgumentException("Quantity to deduct must be greater than zero");
        }
        if (getAvailableQuantity() < amount)
        {
            throw new IllegalStateException(
                    "Cannot deduct more than the available stock; reserved units are not available");
        }
        this.quantity -= amount;
    }

    /**
     * Consumes a quantity that was previously reserved, removing it from both
     * the physical stock and the reserved quantity. This is the counterpart of
     * {@link #reserveStock(int)} for a movement that actually ships the goods:
     * unlike {@link #deductStock(int)} it never touches units reserved for a
     * different holder.
     *
     * @param amount the number of reserved units to consume (must be &gt; 0)
     * @throws IllegalArgumentException if {@code amount} is &le; 0
     * @throws IllegalStateException    if {@code amount} exceeds the reserved quantity
     */
    public void consumeReservedStock(int amount)
    {
        if (amount <= 0)
        {
            throw new IllegalArgumentException("Quantity to consume must be greater than zero");
        }
        if (this.reservedQuantity < amount)
        {
            throw new IllegalStateException("Cannot consume more than currently reserved stock");
        }
        this.reservedQuantity -= amount;
        this.quantity -= amount;
    }

    /**
     * Adds a quantity to the physical stock on hand.
     *
     * @param amount the number of units to add (must be &gt; 0)
     * @throws IllegalArgumentException if {@code amount} is &le; 0, or if the
     *                                  resulting quantity would not fit in an {@code int}
     */
    public void addStock(int amount)
    {
        if (amount <= 0)
        {
            throw new IllegalArgumentException("Quantity to add must be greater than zero");
        }
        try
        {
            this.quantity = Math.addExact(this.quantity, amount);
        } catch (ArithmeticException exception)
        {
            throw new IllegalArgumentException("Quantity would exceed the maximum supported value", exception);
        }
    }

    /**
     * Changes the low-stock threshold.
     *
     * <p>The threshold is independent of the quantity on hand, so it can be
     * retuned at any time without touching stock. A threshold above the current
     * available quantity simply makes the record report as low stock, which is
     * a legitimate configuration rather than an error.</p>
     *
     * @param minThreshold the new minimum quantity threshold (must be &ge; 0)
     * @throws IllegalArgumentException if {@code minThreshold} is negative
     */
    public void updateMinThreshold(int minThreshold)
    {
        if (minThreshold < 0)
        {
            throw new IllegalArgumentException("Minimum threshold cannot be negative");
        }
        this.minThreshold = minThreshold;
    }

    /**
     * Indicates whether the available quantity is at or below the minimum threshold.
     *
     * @return true if the available quantity equals or falls below the minimum threshold, false otherwise
     */
    public boolean isBelowThreshold()
    {
        return getAvailableQuantity() <= this.minThreshold;
    }

    /**
     * @return the inventory identifier
     */
    public UUID getId()
    {
        return id;
    }

    /**
     * @return the location where the stock is held
     */
    public Location getLocation()
    {
        return location;
    }

    /**
     * @return the product being stocked
     */
    public Product getProduct()
    {
        return product;
    }

    /**
     * @return the physical quantity on hand
     */
    public Integer getQuantity()
    {
        return quantity;
    }

    /**
     * @return the quantity reserved for pending orders
     */
    public Integer getReservedQuantity()
    {
        return reservedQuantity;
    }

    /**
     * @return the minimum quantity threshold
     */
    public Integer getMinThreshold()
    {
        return minThreshold;
    }

    /**
     * @return the optimistic-locking version
     */
    public Long getVersion()
    {
        return version;
    }
}
