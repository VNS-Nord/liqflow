package com.vnsnord.liqflow.inventory;

import com.vnsnord.liqflow.location.Location;
import com.vnsnord.liqflow.product.Product;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Inventory} entities to their response DTOs.
 */
@Component
public class InventoryMapper
{
    /**
     * Converts an inventory entity into a response DTO, denormalizing both the
     * holding location and the stocked product.
     *
     * @param inventory the inventory entity
     * @return the response DTO
     */
    public InventoryResponse toResponse(Inventory inventory)
    {
        Location location = inventory.getLocation();
        Product product = inventory.getProduct();

        return new InventoryResponse(inventory.getId(),
                location.getId(),
                location.getCode(),
                product.getId(),
                product.getSku(),
                product.getName(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getMinThreshold());
    }
}