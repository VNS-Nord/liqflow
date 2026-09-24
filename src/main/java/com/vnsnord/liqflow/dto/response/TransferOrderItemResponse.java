package com.vnsnord.liqflow.dto.response;

import java.util.UUID;

/**
 * Response payload describing a single line item of a transfer order.
 *
 * @param id          the item identifier
 * @param productId   the identifier of the transferred product
 * @param productSku  the SKU of the transferred product
 * @param productName the display name of the transferred product
 * @param quantity    the number of units to transfer
 */
public record TransferOrderItemResponse(UUID id,
                                        UUID productId,
                                        String productSku,
                                        String productName,
                                        int quantity)
{
}