package com.vnsnord.liqflow.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for changing an inventory record's minimum threshold.
 *
 * <p>The threshold is the only directly editable inventory field. The physical
 * quantity is deliberately absent: it may only move through
 * {@code add-stock}/{@code deduct-stock} and through transfer orders, so that
 * the quantity and the reservations backing it cannot drift apart.</p>
 *
 * @param minThreshold the new low-stock threshold (must be &ge; 0)
 */
public record UpdateInventoryRequest(@NotNull(message = "Minimum threshold mandatory")
                                     @Min(value = 0, message = "Minimum threshold cannot be negative")
                                     Integer minThreshold)
{
}
