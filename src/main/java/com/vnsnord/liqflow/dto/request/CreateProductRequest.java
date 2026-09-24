package com.vnsnord.liqflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request payload for creating a new product.
 *
 * @param sku         the unique stock keeping unit (must not be blank, max 50 characters)
 * @param name        the display name of the product (must not be blank, max 100 characters)
 * @param description an optional description, or null
 * @param price       the selling price (must be non-negative)
 */
public record CreateProductRequest(@NotBlank(message = "SKU mandatory")
                                   @Size(max = 50, message = "SKU cannot exceed 50 characters")
                                   String sku,

                                   @NotBlank(message = "Product name mandatory")
                                   @Size(max = 100, message = "Name cannot exceed 100 characters")
                                   String name,

                                   String description,

                                   @NotNull(message = "Price mandatory")
                                   @PositiveOrZero(message = "Price must be non-negative")
                                   BigDecimal price)
{
}