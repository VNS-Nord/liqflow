package com.vnsnord.liqflow.controller;

import com.vnsnord.liqflow.dto.request.CreateInventoryRequest;
import com.vnsnord.liqflow.dto.response.InventoryResponse;
import com.vnsnord.liqflow.dto.request.StockMovementRequest;
import com.vnsnord.liqflow.dto.request.UpdateInventoryRequest;
import com.vnsnord.liqflow.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for reading and manipulating inventory records under
 * {@code /api/v1/inventories}.
 */
@RestController
@RequestMapping("/api/v1/inventories")
public class InventoryController
{
    private final InventoryService inventoryService;

    /**
     * Creates a new inventory controller backed by the given service.
     *
     * @param inventoryService the inventory application service
     */
    public InventoryController(InventoryService inventoryService)
    {
        this.inventoryService = inventoryService;
    }

    /**
     * Returns a single inventory record by its identifier.
     *
     * @param id the inventory identifier
     * @return the inventory record with HTTP 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponse> getById(@PathVariable UUID id)
    {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    /**
     * Returns a page of inventory records held at a location.
     *
     * @param locationId the location identifier
     * @param pageable   pagination and sorting information
     * @return the page of records with HTTP 200
     */
    @GetMapping("/location/{locationId}")
    public ResponseEntity<Page<InventoryResponse>> getByLocation(
            @PathVariable UUID locationId,
            Pageable pageable)
    {
        return ResponseEntity.ok(inventoryService.getInventoryByLocation(locationId, pageable));
    }

    /**
     * Returns the inventory record for a product at a location.
     *
     * @param productId  the product identifier
     * @param locationId the location identifier
     * @return the inventory record with HTTP 200
     */
    @GetMapping("/product/{productId}/location/{locationId}")
    public ResponseEntity<InventoryResponse> getByProductAndLocation(
            @PathVariable UUID productId,
            @PathVariable UUID locationId)
    {
        return ResponseEntity.ok(inventoryService.getInventoryByProductAndLocation(productId, locationId));
    }

    /**
     * Returns all inventory records whose available quantity is at or below
     * their configured minimum threshold.
     *
     * @return the list of low-stock records with HTTP 200
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStock()
    {
        return ResponseEntity.ok(inventoryService.getLowStockInventory());
    }

    /**
     * Returns all inventory records.
     *
     * @return the list of records with HTTP 200
     */
    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAll()
    {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    /**
     * Creates a new inventory record for a product at a location.
     *
     * @param request the creation request
     * @return the created record with a {@code Location} header and HTTP 201
     */
    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest request)
    {
        InventoryResponse response = inventoryService.createInventory(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Changes an inventory record's low-stock threshold.
     *
     * <p>This is the only mutable field on an inventory record that is edited
     * directly. The physical quantity is intentionally not editable here; it
     * moves through {@code add-stock}, {@code deduct-stock} and transfer orders
     * so that it cannot drift out of agreement with the reservations held
     * against it.</p>
     *
     * @param id      the inventory identifier
     * @param request the new threshold
     * @return the updated record with HTTP 200
     */
    @PutMapping("/{id}/min-threshold")
    public ResponseEntity<InventoryResponse> updateMinThreshold(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryRequest request)
    {
        return ResponseEntity.ok(inventoryService.updateMinThreshold(id, request));
    }

    /**
     * Adds stock to an inventory record.
     *
     * @param id      the inventory identifier
     * @param request the quantity to add
     * @return the updated record with HTTP 200
     */
    @PostMapping("/{id}/add-stock")
    public ResponseEntity<InventoryResponse> addStock(@PathVariable UUID id,
                                                      @Valid @RequestBody StockMovementRequest request)
    {
        return ResponseEntity.ok(inventoryService.addStock(id, request.quantity()));
    }

    /**
     * Deducts stock from an inventory record.
     *
     * @param id      the inventory identifier
     * @param request the quantity to deduct
     * @return the updated record with HTTP 200
     */
    @PostMapping("/{id}/deduct-stock")
    public ResponseEntity<InventoryResponse> deductStock(@PathVariable UUID id,
                                                         @Valid @RequestBody StockMovementRequest request)
    {
        return ResponseEntity.ok(inventoryService.deductStock(id, request.quantity()));
    }
}