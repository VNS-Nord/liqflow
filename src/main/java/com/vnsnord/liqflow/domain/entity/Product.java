package com.vnsnord.liqflow.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a sellable product in the catalog.
 *
 * <p>Each product is identified by a unique {@code sku} and carries a display
 * {@code name}, an optional {@code description}, and a non-negative selling
 * {@code price}.</p>
 *
 * @see Inventory
 */
@Entity
@Table(
        name = "products",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_sku",
                columnNames = {"sku"}
        )
)
public class Product
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected Product()
    {
    }

    /**
     * Creates a new product.
     *
     * @param sku         the unique stock keeping unit (must not be blank)
     * @param name        the display name of the product (must not be blank)
     * @param description an optional description, or null
     * @param price       the selling price (must be non-negative)
     * @throws IllegalArgumentException if {@code sku} or {@code name} is blank, or if {@code price} is negative
     */
    public Product(String sku, String name, String description, BigDecimal price)
    {
        if (sku == null || sku.isBlank())
        {
            throw new IllegalArgumentException("SKU is required");
        }
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Product name is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    /**
     * Updates the display name, description and price of this product.
     *
     * @param name        the new display name (must not be blank)
     * @param description the new description, or null
     * @param price       the new selling price (must be non-negative)
     * @throws IllegalArgumentException if {@code name} is blank or {@code price} is negative
     */
    public void updateDetails(String name, String description, BigDecimal price)
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Product name is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public UUID getId()
    {
        return id;
    }

    public String getSku()
    {
        return sku;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public BigDecimal getPrice()
    {
        return price;
    }
}
