package com.vnsnord.liqflow.domain.entity;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

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

    public int getAvailableQuantity()
    {
        return this.quantity - this.reservedQuantity;
    }

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

    public void addStock(int amount)
    {
        if (amount <= 0)
        {
            throw new IllegalArgumentException("Quantity to add must be greater than zero");
        }
        this.quantity += amount;
    }

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
