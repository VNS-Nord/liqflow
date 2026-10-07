package com.vnsnord.liqflow.product;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request payload for creating a new product.
 *
 * @param sku         the unique stock keeping unit (must not be blank, max 50 characters)
 * @param name        the display name of the product (must not be blank, max 100 characters)
 * @param description an optional description, or null (max 500 characters)
 * @param price       the selling price (must be non-negative, max 10 integer
 *                    digits and 2 fraction digits)
 */
public record CreateProductRequest(@NotBlank(message = "SKU mandatory")
                                   @Size(max = 50, message = "SKU cannot exceed 50 characters")
                                   String sku,

                                   @NotBlank(message = "Product name mandatory")
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