package com.vnsnord.liqflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for adding to, deducting from, reserving, or releasing stock
 * on an existing inventory record.
 *
 * @param quantity the number of units to move (must be greater than zero)
 */
public record StockMovementRequest(@NotNull(message = "Quantity mandatory")
                                   @Min(value = 1, message = "Quantity must be greater than zero")
                                   Integer quantity)
{
}