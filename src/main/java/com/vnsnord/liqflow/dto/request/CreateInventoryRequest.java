package com.vnsnord.liqflow.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload for creating a new inventory record at a location.
 *
 * @param locationId   the identifier of the location holding the stock
 * @param productId    the identifier of the product being stocked
 * @param initialStock the initial physical quantity on hand (must be &ge; 0)
 * @param minThreshold the quantity threshold that signals low stock (must be &ge; 0)
 */
public record CreateInventoryRequest(@NotNull(message = "Location mandatory")
                                     UUID locationId,

                                     @NotNull(message = "Product mandatory")
                                     UUID productId,

                                     @Min(value = 0, message = "Initial stock cannot be negative")
                                     int initialStock,

                                     @Min(value = 0, message = "Minimum threshold cannot be negative")
                                     int minThreshold)
{
}