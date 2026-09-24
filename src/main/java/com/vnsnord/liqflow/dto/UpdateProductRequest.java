package com.vnsnord.liqflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for updating an existing product's mutable fields.
 *
 * <p>The {@code sku} is immutable and is not part of this payload.</p>
 *
 * @param name        the new display name of the product (must not be blank, max 100 characters)
 * @param description the new description, or null to clear it
 * @param price       the new selling price (must be non-negative)
 */
public record UpdateProductRequest(@NotBlank(message = "Product name mandatory")
                                   @Size(max = 100, message = "Name cannot exceed 100 characters")
                                   String name,

                                   String description,

                                   @NotNull(message = "Price mandatory")
                                   @PositiveOrZero(message = "Price must be non-negative")
                                   BigDecimal price)
{
}