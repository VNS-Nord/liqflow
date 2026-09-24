package com.vnsnord.liqflow.service;

import com.vnsnord.liqflow.domain.entity.Inventory;
import com.vnsnord.liqflow.domain.entity.Location;
import com.vnsnord.liqflow.domain.entity.Product;
import com.vnsnord.liqflow.domain.entity.TransferOrder;
import com.vnsnord.liqflow.domain.entity.TransferOrderItem;
import com.vnsnord.liqflow.dto.CreateTransferOrderRequest;
import com.vnsnord.liqflow.dto.TransferOrderDetailResponse;
import com.vnsnord.liqflow.dto.TransferOrderItemRequest;
import com.vnsnord.liqflow.dto.TransferOrderResponse;
import com.vnsnord.liqflow.exception.ConflictException;
import com.vnsnord.liqflow.exception.LocationNotFoundException;
import com.vnsnord.liqflow.exception.ProductNotFoundException;
import com.vnsnord.liqflow.exception.TransferOrderNotFoundException;
import com.vnsnord.liqflow.infrastructure.persistence.InventoryRepository;
import com.vnsnord.liqflow.infrastructure.persistence.LocationRepository;
import com.vnsnord.liqflow.infrastructure.persistence.ProductRepository;
import com.vnsnord.liqflow.infrastructure.persistence.TransferOrderRepository;
import com.vnsnord.liqflow.service.mapper.TransferOrderMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for the full transfer order lifecycle, including item
 * management, status transitions, and the physical stock movement executed on
 * completion.
 */
@Service
@Transactional(readOnly = true)
public class TransferOrderService
{

    private final TransferOrderRepository transferOrderRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final TransferOrderMapper transferOrderMapper;

    /**
     * Creates a new transfer order service with the given dependencies.
     *
     * @param transferOrderRepository the transfer order repository
     * @param locationRepository      the location repository
     * @param productRepository       the product repository
     * @param inventoryRepository     the inventory repository
     * @param transferOrderMapper     the transfer order mapper
     */
    public TransferOrderService(TransferOrderRepository transferOrderRepository,
                                LocationRepository locationRepository,
                                ProductRepository productRepository,
                                InventoryRepository inventoryRepository,
                                TransferOrderMapper transferOrderMapper)
    {
        this.transferOrderRepository = transferOrderRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.transferOrderMapper = transferOrderMapper;
    }

    /**
     * Finds a transfer order by its identifier.
     *
     * @param id the order identifier
     * @return the corresponding summary response DTO
     * @throws TransferOrderNotFoundException if no order exists with the given id
     */
    public TransferOrderResponse getTransferOrderById(UUID id)
    {
        return transferOrderRepository.findById(id)
                .map(transferOrderMapper::toResponse)
                .orElseThrow(() -> new TransferOrderNotFoundException("id", id));
    }

    /**
     * Finds a transfer order together with its line items.
     *
     * @param id the order identifier
     * @return the corresponding detailed response DTO
     * @throws TransferOrderNotFoundException if no order exists with the given id
     */
    public TransferOrderDetailResponse getTransferOrderDetailById(UUID id)
    {
        return transferOrderMapper.toDetailResponse(findWithDetails(id));
    }

    /**
     * Returns a page of all transfer orders.
     *
     * @param pageable pagination and sorting information
     * @return a page of summary response DTOs
     */
    public Page<TransferOrderResponse> getAllTransferOrders(Pageable pageable)
    {
        return transferOrderRepository.findAllWithLocations(pageable)
                .map(transferOrderMapper::toResponse);
    }

/**
     * Creates a new transfer order in {@code DRAFT} status.
     *
     * @param request the creation request
     * @return the response DTO of the saved order
     * @throws ConflictException         if an order with the same number already exists
     * @throws LocationNotFoundException if either location does not exist
     */
    @Transactional
    public TransferOrderResponse createTransferOrder(CreateTransferOrderRequest request) {
        String orderNumber = request.orderNumber().trim();
        if (transferOrderRepository.existsByOrderNumber(orderNumber))
        {
            throw new ConflictException("Transfer order with number '" + orderNumber + "' already exists");
        }
        Location source = locationRepository.findById(request.sourceLocationId())
                .orElseThrow(() -> new LocationNotFoundException("id", request.sourceLocationId()));
        Location target = locationRepository.findById(request.targetLocationId())
                .orElseThrow(() -> new LocationNotFoundException("id", request.targetLocationId()));

        TransferOrder order = new TransferOrder(orderNumber, source, target);
        TransferOrder savedOrder = transferOrderRepository.save(order);
        return transferOrderMapper.toResponse(savedOrder);
    }

    /**
     * Adds a product line to a draft transfer order.
     *
     * @param orderId the order identifier
     * @param request the item request
     * @return the updated detailed response DTO
     * @throws TransferOrderNotFoundException if the order does not exist
     * @throws ProductNotFoundException       if the product does not exist
     */
    @Transactional
    public TransferOrderDetailResponse addItem(UUID orderId, TransferOrderItemRequest request) {
        TransferOrder order = findWithDetails(orderId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException("id", request.productId()));
        order.addItem(product, request.quantity());
        return transferOrderMapper.toDetailResponse(transferOrderRepository.save(order));
    }

    /**
     * Removes a line item from a draft transfer order.
     *
     * @param orderId the order identifier
     * @param itemId  the line item identifier
     * @return the updated detailed response DTO
     * @throws TransferOrderNotFoundException if the order or the item does not exist
     */
    @Transactional
    public TransferOrderDetailResponse removeItem(UUID orderId, UUID itemId) {
        TransferOrder order = findWithDetails(orderId);
        TransferOrderItem item = order.getItems().stream()
                .filter(candidate -> candidate.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new TransferOrderNotFoundException("item id", itemId));
        order.removeItem(item);
        return transferOrderMapper.toDetailResponse(transferOrderRepository.save(order));
    }

    /**
     * Submits a draft transfer order, transitioning it to {@code SUBMITTED}.
     *
     * @param id the order identifier
     * @return the response DTO of the updated order
     * @throws TransferOrderNotFoundException if the order does not exist
     * @throws IllegalStateException          if the order is not in draft status or has no items
     */
    @Transactional
    public TransferOrderResponse submit(UUID id) {
        TransferOrder order = findWithDetails(id);
        order.submit();
        return transferOrderMapper.toResponse(transferOrderRepository.save(order));
    }

    /**
     * Marks a submitted transfer order as in transit.
     *
     * @param id the order identifier
     * @return the response DTO of the updated order
     * @throws TransferOrderNotFoundException if the order does not exist
     * @throws IllegalStateException          if the order is not in submitted status
     */
    @Transactional
    public TransferOrderResponse markInTransit(UUID id) {
        TransferOrder order = findWithDetails(id);
        order.markInTransit();
        return transferOrderMapper.toResponse(transferOrderRepository.save(order));
    }

    /**
     * Completes an in-transit transfer order, deducting each item from the
     * source inventory and adding it to the target inventory, then
     * transitioning the order to {@code COMPLETED}.
     *
     * @param id the order identifier
     * @return the response DTO of the updated order
     * @throws TransferOrderNotFoundException if the order does not exist
     * @throws IllegalStateException          if the order is not in transit, or if either side
     *                                        lacks an inventory record or sufficient stock
     */
    @Transactional
    public TransferOrderResponse complete(UUID id) {
        TransferOrder order = findWithDetails(id);
        moveStock(order);
        order.complete();
        return transferOrderMapper.toResponse(transferOrderRepository.save(order));
    }

    /**
     * Cancels a transfer order.
     *
     * @param id the order identifier
     * @return the response DTO of the updated order
     * @throws TransferOrderNotFoundException if the order does not exist
     * @throws IllegalStateException          if the order is already completed
     */
    @Transactional
    public TransferOrderResponse cancel(UUID id) {
        TransferOrder order = findWithDetails(id);
        order.cancel();
        return transferOrderMapper.toResponse(transferOrderRepository.save(order));
    }

    /**
     * Physically moves the ordered quantities from the source to the target
     * inventory records.
     *
     * @param order the transfer order being completed
     * @throws IllegalStateException if either side has no inventory record for an item's product
     */
    private void moveStock(TransferOrder order) {
        for (TransferOrderItem item : order.getItems()) {
            Product product = item.getProduct();
            Inventory sourceInventory = inventoryRepository
                    .findByProductIdAndLocationIdForUpdate(product.getId(), order.getSourceLocation().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("No source inventory for product '%s' at location '%s'",
                                    product.getSku(), order.getSourceLocation().getCode())));
            Inventory targetInventory = inventoryRepository
                    .findByProductIdAndLocationIdForUpdate(product.getId(), order.getTargetLocation().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("No target inventory for product '%s' at location '%s'",
                                    product.getSku(), order.getTargetLocation().getCode())));
            sourceInventory.deductStock(item.getQuantity());
            targetInventory.addStock(item.getQuantity());
        }
    }

    /**
     * Loads a transfer order with its items and locations eagerly to avoid
     * lazy-loading issues, or throws when it does not exist.
     *
     * @param id the order identifier
     * @return the transfer order entity
     * @throws TransferOrderNotFoundException if no order exists with the given id
     */
    private TransferOrder findWithDetails(UUID id) {
        return transferOrderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new TransferOrderNotFoundException("id", id));
    }
}