package com.vnsnord.liqflow.domain.entity;

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
 * released, deducted, or added through the domain methods on this class, all of
 * which enforce the required invariants.</p>
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
     * @param location      the location where the stock is held (must not be null)
     * @param product       the product being stocked (must not be null)
     * @param initialStock  the initial physical quantity on hand (must be &ge; 0)
     * @param minThreshold  the minimum quantity threshold (must be &ge; 0)
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
        this.location = Objects.requireNonNull(location,  "Location cannot be null");
        this.product = Objects.requireNonNull(product,  "Product cannot be null");
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
     * Deducts a quantity from the physical stock on hand. If the deducted amount
     * is covered by reserved stock, the reserved quantity is reduced accordingly.
     *
     * @param amount the number of units to deduct (must be &gt; 0)
     * @throws IllegalArgumentException if {@code amount} is &le; 0
     * @throws IllegalStateException    if there is insufficient physical stock
     */
    public void deductStock(int amount)
    {
        if (amount <= 0)
        {
            throw new IllegalArgumentException("Quantity to deduct must be greater than zero");
        }
        if (this.quantity < amount)
        {
            throw new IllegalStateException("Insufficient physical stock to deduct");
        }
        this.quantity -= amount;
        if (this.reservedQuantity >= amount)
        {
            this.reservedQuantity -= amount;
        }
    }

    /**
     * Adds a quantity to the physical stock on hand.
     *
     * @param amount the number of units to add (must be &gt; 0)
     * @throws IllegalArgumentException if {@code amount} is &le; 0
     */
    public void addStock(int amount)
    {
        if (amount <= 0)
        {
            throw new IllegalArgumentException("Quantity to add must be greater than zero");
        }
        this.quantity += amount;
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

    public UUID getId()
    {
        return id;
    }

    public Location getLocation()
    {
        return location;
    }

    public Product getProduct()
    {
        return product;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public Integer getReservedQuantity()
    {
        return reservedQuantity;
    }

    public Integer getMinThreshold()
    {
        return minThreshold;
    }

    public Long getVersion()
    {
        return version;
    }
}
