package com.vnsnord.liqflow.service.mapper;

import com.vnsnord.liqflow.domain.entity.Inventory;
import com.vnsnord.liqflow.domain.entity.Location;
import com.vnsnord.liqflow.domain.entity.Product;
import com.vnsnord.liqflow.dto.InventoryResponse;
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