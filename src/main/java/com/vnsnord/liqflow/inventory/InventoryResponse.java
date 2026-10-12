package com.vnsnord.liqflow.inventory;

import java.util.UUID;

/**
 * Response payload describing an inventory record.
 *
 * @param id                the inventory identifier
 * @param locationId        the identifier of the holding location
 * @param locationCode      the code of the holding location
 * @param productId         the identifier of the stocked product
 * @param productSku        the SKU of the stocked product
 * @param productName       the display name of the stocked product
 * @param quantity          the physical quantity on hand
 * @param reservedQuantity  the quantity reserved for pending orders
 * @param availableQuantity the quantity available for new reservations
 * @param minThreshold      the quantity threshold that signals low stock
 */
public record InventoryResponse(UUID id,
                                UUID locationId,
                                String locationCode,
                                UUID productId,
                                String productSku,
                                String productName,
                                int quantity,
                                int reservedQuantity,
                                int availableQuantity,
                                int minThreshold)
{
}