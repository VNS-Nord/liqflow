package com.vnsnord.liqflow.service.mapper;

import com.vnsnord.liqflow.domain.entity.Location;
import com.vnsnord.liqflow.domain.entity.Product;
import com.vnsnord.liqflow.domain.entity.TransferOrder;
import com.vnsnord.liqflow.domain.entity.TransferOrderItem;
import com.vnsnord.liqflow.dto.response.TransferOrderDetailResponse;
import com.vnsnord.liqflow.dto.response.TransferOrderItemResponse;
import com.vnsnord.liqflow.dto.response.TransferOrderResponse;
import org.springframework.stereotype.Component;

/**
 * Maps {@link TransferOrder} entities and their items to response DTOs.
 */
@Component
public class TransferOrderMapper
{
    /**
     * Converts a transfer order entity into a summary response DTO.
     *
     * @param entity the transfer order entity
     * @return the summary response DTO
     */
    public TransferOrderResponse toResponse(TransferOrder entity) {
        Location source = entity.getSourceLocation();
        Location target = entity.getTargetLocation();

        return new TransferOrderResponse(
                entity.getId(),
                entity.getOrderNumber(),
                source.getId(),
                source.getCode(),
                target.getId(),
                target.getCode(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    /**
     * Converts a transfer order entity into a detailed response DTO including
     * all of its line items.
     *
     * @param entity the transfer order entity
     * @return the detailed response DTO
     */
    public TransferOrderDetailResponse toDetailResponse(TransferOrder entity) {
        Location source = entity.getSourceLocation();
        Location target = entity.getTargetLocation();

        return new TransferOrderDetailResponse(
                entity.getId(),
                entity.getOrderNumber(),
                source.getId(),
                source.getCode(),
                target.getId(),
                target.getCode(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getItems().stream().map(this::toItemResponse).toList()
        );
    }

    /**
     * Converts a transfer order item into its response DTO, denormalizing the
     * related product.
     *
     * @param item the transfer order item
     * @return the response DTO
     */
    public TransferOrderItemResponse toItemResponse(TransferOrderItem item) {
        Product product = item.getProduct();
        return new TransferOrderItemResponse(
                item.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                item.getQuantity()
        );
    }
}