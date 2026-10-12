package com.vnsnord.liqflow.transfer;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST endpoints for the transfer order lifecycle under
 * {@code /api/v1/transfer-orders}.
 */
@RestController
@RequestMapping("/api/v1/transfer-orders")
public class TransferOrderController
{
    private final TransferOrderService transferOrderService;

    /**
     * Creates a new transfer order controller backed by the given service.
     *
     * @param transferOrderService the transfer order application service
     */
    public TransferOrderController(TransferOrderService transferOrderService)
    {
        this.transferOrderService = transferOrderService;
    }

    /**
     * Returns a single transfer order together with its line items.
     *
     * @param id the order identifier
     * @return the detailed order with HTTP 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransferOrderDetailResponse> getById(@PathVariable UUID id)
    {
        return ResponseEntity.ok(transferOrderService.getTransferOrderDetailById(id));
    }

    /**
     * Returns a page of all transfer orders.
     *
     * @param pageable pagination and sorting information
     * @return the page of orders with HTTP 200
     */
    @GetMapping
    public ResponseEntity<Page<TransferOrderResponse>> getAll(Pageable pageable)
    {
        return ResponseEntity.ok(transferOrderService.getAllTransferOrders(pageable));
    }

    /**
     * Creates a new transfer order in draft status.
     *
     * @param request the creation request
     * @return the created order with a {@code Location} header and HTTP 201
     */
    @PostMapping
    public ResponseEntity<TransferOrderResponse> createTransferOrder(@Valid @RequestBody CreateTransferOrderRequest request)
    {
        TransferOrderResponse response = transferOrderService.createTransferOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Adds a product line to a draft transfer order.
     *
     * @param id      the order identifier
     * @param request the item to add
     * @return the updated detailed order with HTTP 200
     */
    @PostMapping("/{id}/items")
    public ResponseEntity<TransferOrderDetailResponse> addItem(@PathVariable UUID id,
                                                               @Valid @RequestBody TransferOrderItemRequest request)
    {
        return ResponseEntity.ok(transferOrderService.addItem(id, request));
    }

    /**
     * Removes a line item from a draft transfer order.
     *
     * @param id     the order identifier
     * @param itemId the line item identifier
     * @return the updated detailed order with HTTP 200
     */
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<TransferOrderDetailResponse> removeItem(@PathVariable UUID id,
                                                                  @PathVariable UUID itemId)
    {
        return ResponseEntity.ok(transferOrderService.removeItem(id, itemId));
    }

    /**
     * Submits a draft transfer order.
     *
     * @param id the order identifier
     * @return the updated order with HTTP 200
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<TransferOrderResponse> submit(@PathVariable UUID id)
    {
        return ResponseEntity.ok(transferOrderService.submit(id));
    }

    /**
     * Marks a submitted transfer order as in transit.
     *
     * @param id the order identifier
     * @return the updated order with HTTP 200
     */
    @PostMapping("/{id}/in-transit")
    public ResponseEntity<TransferOrderResponse> markInTransit(@PathVariable UUID id)
    {
        return ResponseEntity.ok(transferOrderService.markInTransit(id));
    }

    /**
     * Completes an in-transit transfer order, moving the stock between
     * locations.
     *
     * @param id the order identifier
     * @return the updated order with HTTP 200
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<TransferOrderResponse> complete(@PathVariable UUID id)
    {
        return ResponseEntity.ok(transferOrderService.complete(id));
    }

    /**
     * Cancels a transfer order.
     *
     * @param id the order identifier
     * @return the updated order with HTTP 200
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransferOrderResponse> cancel(@PathVariable UUID id)
    {
        return ResponseEntity.ok(transferOrderService.cancel(id));
    }
}