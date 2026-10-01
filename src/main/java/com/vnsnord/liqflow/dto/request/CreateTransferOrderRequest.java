package com.vnsnord.liqflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request payload for creating a new transfer order.
 *
 * <p>Line items are intentionally not part of the initial creation; they are
 * added afterward through the item endpoints while the order is still a
 * draft.</p>
 *
 * @param orderNumber      unique business identifier for the order (must not be blank, max 60 characters)
 * @param sourceLocationId the identifier of the location to transfer from
 * @param targetLocationId the identifier of the location to transfer to
 */
public record CreateTransferOrderRequest(@NotBlank(message = "Order number mandatory")
                                         @Size(max = 60, message = "Order number cannot exceed 60 characters")
                                         String orderNumber,

                                         @NotNull(message = "Source location mandatory")
                                         UUID sourceLocationId,

                                         @NotNull(message = "Target location mandatory")
                                         UUID targetLocationId)
{
}