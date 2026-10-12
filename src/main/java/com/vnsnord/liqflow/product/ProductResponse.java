package com.vnsnord.liqflow.product;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response payload describing a product.
 *
 * @param id          the product identifier
 * @param sku         the unique stock keeping unit
 * @param name        the display name of the product
 * @param description an optional description, or null
 * @param price       the selling price
 */
public record ProductResponse(UUID id,
                              String sku,
                              String name,
                              String description,
                              BigDecimal price)
{
}