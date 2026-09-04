package com.vnsnord.liqflow.domain.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

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

    @Column(nullable = false, unique = true, length = 50)
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
