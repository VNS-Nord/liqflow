package com.vnsnord.liqflow.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload for creating a new inventory record at a location.
 *
 * <p>{@code initialStock} and {@code minThreshold} are declared as boxed
 * {@link Integer} rather than {@code int} on purpose: with a primitive, a client
 * that omits the field entirely binds it to {@code 0} and passes validation,
 * silently creating a zero-stock record with a zero threshold. With
 * {@code @NotNull} the omission is reported as a 400 instead.</p>
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

                                     @NotNull(message = "Initial stock mandatory")
                                     @Min(value = 0, message = "Initial stock cannot be negative")
                                     Integer initialStock,

                                     @NotNull(message = "Minimum threshold mandatory")
                                     @Min(value = 0, message = "Minimum threshold cannot be negative")
                                     Integer minThreshold)
{
}
