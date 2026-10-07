package com.vnsnord.liqflow.product;

import jakarta.validation.constraints.Digits;
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
 * @param description the new description, or null to clear it (max 500 characters)
 * @param price       the new selling price (must be non-negative, max 10 integer
 *                    digits and 2 fraction digits)
 */
public record UpdateProductRequest(@NotBlank(message = "Product name mandatory")
                                   @Size(max = 100, message = "Name cannot exceed 100 characters")
                                   String name,

                                   @Size(max = 500, message = "Description cannot exceed 500 characters")
                                   String description,

                                   @NotNull(message = "Price mandatory")
                                   @PositiveOrZero(message = "Price must be non-negative")
                                   @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer and 2 fraction digits")
                                   BigDecimal price)
{
}