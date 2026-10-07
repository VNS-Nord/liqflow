package com.vnsnord.liqflow.inventory;

import com.vnsnord.liqflow.common.exception.ConflictException;
import com.vnsnord.liqflow.common.exception.InventoryNotFoundException;
import com.vnsnord.liqflow.common.exception.LocationNotFoundException;
import com.vnsnord.liqflow.common.exception.ProductNotFoundException;
import com.vnsnord.liqflow.location.Location;
import com.vnsnord.liqflow.location.LocationRepository;
import com.vnsnord.liqflow.product.Product;
import com.vnsnord.liqflow.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for reading and manipulating inventory records.
 */
@Service
@Transactional(readOnly = true)
public class InventoryService
{
    private final InventoryRepository inventoryRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;

    /**
     * Creates a new inventory service with the given dependencies.
     *
     * @param inventoryRepository the inventory repository
     * @param locationRepository  the location repository
     * @param productRepository   the product repository
     * @param inventoryMapper     the inventory mapper
     */
    public InventoryService(InventoryRepository inventoryRepository,
                            LocationRepository locationRepository,
                            ProductRepository productRepository,
                            InventoryMapper inventoryMapper)
    {
        this.inventoryRepository = inventoryRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.inventoryMapper = inventoryMapper;
    }

    /**
     * Finds an inventory record by its identifier.
     *
     * @param id the inventory identifier
     * @return the corresponding response DTO
     * @throws InventoryNotFoundException if no record exists with the given id
     */
    public InventoryResponse getInventoryById(UUID id)
    {
        return inventoryRepository.findById(id)
                .map(inventoryMapper::toResponse)
                .orElseThrow(() -> new InventoryNotFoundException("id", id));
    }

    /**
     * Finds the inventory record for a product at a location.
     *
     * @param productId  the product identifier
     * @param locationId the location identifier
     * @return the corresponding response DTO
     * @throws InventoryNotFoundException if no record exists for the combination
     */
    public InventoryResponse getInventoryByProductAndLocation(UUID productId, UUID locationId)
    {
        return inventoryRepository.findByProductIdAndLocationId(productId, locationId)
                .map(inventoryMapper::toResponse)
                .orElseThrow(() -> new InventoryNotFoundException(
                        String.format("productId '%s' and locationId '%s'", productId, locationId)
                ));
    }

    /**
     * Returns a page of inventory records held at a location.
     *
     * @param locationId the location identifier
     * @param pageable   pagination and sorting information
     * @return a page of response DTOs
     */
    public Page<InventoryResponse> getInventoryByLocation(UUID locationId, Pageable pageable)
    {
        return inventoryRepository.findByLocationId(locationId, pageable)
                .map(inventoryMapper::toResponse);
    }

    /**
     * Returns all inventory records.
     *
     * @return a list of response DTOs
     */
    public List<InventoryResponse> getAllInventory()
    {
        return inventoryRepository.findAllWithDetails().stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    /**
     * Returns all inventory records whose available quantity is at or below
     * their configured minimum threshold.
     *
     * @return a list of low-stock response DTOs
     */
    public List<InventoryResponse> getLowStockInventory()
    {
        return inventoryRepository.findLowStock().stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    /**
     * Creates a new inventory record for a product at a location.
     *
     * @param request the creation request
     * @return the response DTO of the saved record
     * @throws ConflictException         if a record already exists for the product/location combination
     * @throws LocationNotFoundException if the location does not exist
     * @throws ProductNotFoundException  if the product does not exist
     */
    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request)
    {
        if (inventoryRepository.existsByProductIdAndLocationId(request.productId(), request.locationId()))
        {
            throw new ConflictException(
                    String.format("Inventory already exists for productId '%s' and locationId '%s'",
                            request.productId(), request.locationId())
            );
        }

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new LocationNotFoundException("id", request.locationId()));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException("id", request.productId()));

        Inventory inventory = new Inventory(
                location,
                product,
                request.initialStock(),
                request.minThreshold()
        );
        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    /**
     * Changes an inventory record's low-stock threshold.
     *
     * <p>The physical quantity is not editable here on purpose: it may only move
     * through {@link #addStock(UUID, int)} and {@link #deductStock(UUID, int)}
     * so that the quantity and the reservations backing it cannot drift apart.</p>
     *
     * @param id      the inventory identifier
     * @param request the new threshold
     * @return the response DTO of the updated record
     * @throws InventoryNotFoundException if no record exists with the given id
     */
    @Transactional
    public InventoryResponse updateMinThreshold(UUID id, UpdateInventoryRequest request)
    {
        Inventory inventory = getInventory(id);
        inventory.updateMinThreshold(request.minThreshold());
        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    /**
     * Adds stock to the physical quantity on hand.
     *
     * @param id       the inventory identifier
     * @param quantity the number of units to add
     * @return the response DTO of the updated record
     * @throws InventoryNotFoundException if no record exists with the given id
     */
    @Transactional
    public InventoryResponse addStock(UUID id, int quantity)
    {
        Inventory inventory = getInventory(id);
        inventory.addStock(quantity);
        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    /**
     * Deducts stock from the physical quantity on hand.
     *
     * @param id       the inventory identifier
     * @param quantity the number of units to deduct
     * @return the response DTO of the updated record
     * @throws InventoryNotFoundException if no record exists with the given id
     * @throws IllegalStateException      if there is insufficient physical stock
     */
    @Transactional
    public InventoryResponse deductStock(UUID id, int quantity)
    {
        Inventory inventory = getInventory(id);
        inventory.deductStock(quantity);
        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    /**
     * Loads an inventory record by identifier or throws when it does not exist.
     *
     * @param id the inventory identifier
     * @return the inventory entity
     * @throws InventoryNotFoundException if no record exists with the given id
     */
    private Inventory getInventory(UUID id)
    {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("id", id));
    }
}