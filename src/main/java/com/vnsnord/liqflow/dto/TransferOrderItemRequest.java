package com.vnsnord.liqflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload for adding a product line to a draft transfer order.
 *
 * @param productId the identifier of the product to transfer
 * @param quantity  the number of units to transfer (must be greater than zero)
 */
public record TransferOrderItemRequest(@NotNull(message = "Product mandatory")
                                       UUID productId,

                                       @NotNull(message = "Quantity mandatory")
                                       @Min(value = 1, message = "Quantity must be greater than zero")
                                       Integer quantity)
{
}