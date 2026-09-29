package com.vnsnord.liqflow.service;

import com.vnsnord.liqflow.domain.entity.Inventory;
import com.vnsnord.liqflow.domain.entity.Location;
import com.vnsnord.liqflow.domain.entity.Product;
import com.vnsnord.liqflow.domain.enums.LocationType;
import com.vnsnord.liqflow.dto.request.CreateInventoryRequest;
import com.vnsnord.liqflow.dto.response.InventoryResponse;
import com.vnsnord.liqflow.exception.ConflictException;
import com.vnsnord.liqflow.exception.InventoryNotFoundException;
import com.vnsnord.liqflow.infrastructure.persistence.InventoryRepository;
import com.vnsnord.liqflow.infrastructure.persistence.LocationRepository;
import com.vnsnord.liqflow.infrastructure.persistence.ProductRepository;
import com.vnsnord.liqflow.service.mapper.InventoryMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest
{
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryMapper inventoryMapper;
    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void getInventoryById_ShouldReturnInventoryResponseDto_WhenInventoryExists()
    {
        // Given
        UUID inventoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        Product product = createProduct(productId);
        Location location = createLocation(locationId);
        Inventory inventory = createInventory(inventoryId, location, product, 100, 20, 10);

        InventoryResponse expectedDto = new InventoryResponse(
                inventoryId,
                locationId,
                "WH-MAIN",
                productId,
                "SKU-001",
                "Laptop",
                100,
                20,
                80,
                10
        );

        Mockito.when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        Mockito.when(inventoryMapper.toResponse(inventory)).thenReturn(expectedDto);

        // When
        InventoryResponse actualDto = inventoryService.getInventoryById(inventoryId);

        // Then
        Assertions.assertNotNull(actualDto);
        Assertions.assertEquals(expectedDto.id(), actualDto.id());
        Assertions.assertEquals(expectedDto.locationId(), actualDto.locationId());
        Assertions.assertEquals(expectedDto.productId(), actualDto.productId());
        Assertions.assertEquals(expectedDto.quantity(), actualDto.quantity());
        Assertions.assertEquals(expectedDto.availableQuantity(), actualDto.availableQuantity());

        Mockito.verify(inventoryRepository).findById(inventoryId);
        Mockito.verify(inventoryMapper).toResponse(inventory);
    }

    @Test
    void getInventoryById_ShouldThrowInventoryNotFoundException_WhenInventoryDoesNotExist()
    {
        // Given
        UUID inventoryId = UUID.randomUUID();
        Mockito.when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.empty());

        // When & Then
        InventoryNotFoundException exception = Assertions.assertThrows(
                InventoryNotFoundException.class,
                () -> inventoryService.getInventoryById(inventoryId)
        );

        Assertions.assertEquals("Inventory not found with id: '" + inventoryId + "'", exception.getMessage());

        Mockito.verify(inventoryRepository).findById(inventoryId);
        Mockito.verifyNoInteractions(inventoryMapper);
    }

    @Test
    void getInventoryByProductAndLocation_ShouldReturnInventoryResponseDto_WhenInventoryExists()
    {
        // Given
        UUID inventoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        Product product = createProduct(productId);
        Location location = createLocation(locationId);
        Inventory inventory = createInventory(inventoryId, location, product, 50, 5, 5);

        InventoryResponse expectedDto = new InventoryResponse(
                inventoryId,
                locationId,
                "WH-MAIN",
                productId,
                "SKU-001",
                "Laptop",
                50,
                5,
                45,
                5
        );

        Mockito.when(inventoryRepository.findByProductIdAndLocationId(productId, locationId))
                .thenReturn(Optional.of(inventory));
        Mockito.when(inventoryMapper.toResponse(inventory)).thenReturn(expectedDto);

        // When
        InventoryResponse actualDto = inventoryService.getInventoryByProductAndLocation(productId, locationId);

        // Then
        Assertions.assertNotNull(actualDto);
        Assertions.assertEquals(expectedDto.id(), actualDto.id());
        Assertions.assertEquals(expectedDto.locationId(), actualDto.locationId());
        Assertions.assertEquals(expectedDto.productId(), actualDto.productId());

        Mockito.verify(inventoryRepository).findByProductIdAndLocationId(productId, locationId);
        Mockito.verify(inventoryMapper).toResponse(inventory);
    }

    @Test
    void getInventoryByProductAndLocation_ShouldThrowInventoryNotFoundException_WhenInventoryDoesNotExist()
    {
        // Given
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        Mockito.when(inventoryRepository.findByProductIdAndLocationId(productId, locationId))
                .thenReturn(Optional.empty());

        // When & Then
        InventoryNotFoundException exception = Assertions.assertThrows(
                InventoryNotFoundException.class,
                () -> inventoryService.getInventoryByProductAndLocation(productId, locationId)
        );

        Assertions.assertTrue(exception.getMessage().contains(productId.toString()));
        Assertions.assertTrue(exception.getMessage().contains(locationId.toString()));

        Mockito.verify(inventoryRepository).findByProductIdAndLocationId(productId, locationId);
        Mockito.verifyNoInteractions(inventoryMapper);
    }

    @Test
    void getInventoryByLocation_ShouldReturnPagedInventoryResponseDtos()
    {
        // Given
        UUID locationId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Product product = createProduct(UUID.randomUUID());
        Location location = createLocation(locationId);
        Inventory inventory = createInventory(UUID.randomUUID(), location, product, 30, 0, 5);

        InventoryResponse dto = new InventoryResponse(
                inventory.getId(),
                locationId,
                "WH-MAIN",
                product.getId(),
                "SKU-001",
                "Laptop",
                30,
                0,
                30,
                5
        );

        Page<Inventory> inventoryPage = new PageImpl<>(List.of(inventory), pageable, 1);

        Mockito.when(inventoryRepository.findByLocationId(locationId, pageable)).thenReturn(inventoryPage);
        Mockito.when(inventoryMapper.toResponse(inventory)).thenReturn(dto);

        // When
        Page<InventoryResponse> result = inventoryService.getInventoryByLocation(locationId, pageable);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals(1, result.getContent().size());
        Assertions.assertEquals(locationId, result.getContent().getFirst().locationId());

        Mockito.verify(inventoryRepository).findByLocationId(locationId, pageable);
        Mockito.verify(inventoryMapper).toResponse(inventory);
    }

    @Test
    void getAllInventory_ShouldReturnListOfInventoryResponseDtos()
    {
        // Given
        Product product = createProduct(UUID.randomUUID());
        Location location = createLocation(UUID.randomUUID());
        Inventory inventory = createInventory(UUID.randomUUID(), location, product, 10, 0, 2);

        InventoryResponse dto = new InventoryResponse(
                inventory.getId(),
                location.getId(),
                "WH-MAIN",
                product.getId(),
                "SKU-001",
                "Laptop",
                10,
                0,
                10,
                2
        );

        Mockito.when(inventoryRepository.findAllWithDetails()).thenReturn(List.of(inventory));
        Mockito.when(inventoryMapper.toResponse(inventory)).thenReturn(dto);

        // When
        List<InventoryResponse> result = inventoryService.getAllInventory();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());

        Mockito.verify(inventoryRepository).findAllWithDetails();
        Mockito.verify(inventoryMapper).toResponse(inventory);
    }

    @Test
    void getLowStockInventory_ShouldReturnListOfLowStockInventoryResponseDtos()
    {
        // Given
        Product product = createProduct(UUID.randomUUID());
        Location location = createLocation(UUID.randomUUID());
        Inventory inventory = createInventory(UUID.randomUUID(), location, product, 2, 0, 5);

        InventoryResponse dto = new InventoryResponse(
                inventory.getId(),
                location.getId(),
                "WH-MAIN",
                product.getId(),
                "SKU-001",
                "Laptop",
                2,
                0,
                2,
                5
        );

        Mockito.when(inventoryRepository.findLowStock()).thenReturn(List.of(inventory));
        Mockito.when(inventoryMapper.toResponse(inventory)).thenReturn(dto);

        // When
        List<InventoryResponse> result = inventoryService.getLowStockInventory();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(inventory.getId(), result.getFirst().id());

        Mockito.verify(inventoryRepository).findLowStock();
        Mockito.verify(inventoryMapper).toResponse(inventory);
    }

    @Test
    void createInventory_ShouldSaveAndReturnInventoryResponse()
    {
        // Given
        UUID locationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateInventoryRequest request = new CreateInventoryRequest(locationId, productId, 25, 5);

        Product product = createProduct(productId);
        Location location = createLocation(locationId);
        Inventory inventoryToSave = new Inventory(location, product, 25, 5);
        Inventory saved = createInventory(UUID.randomUUID(), location, product, 25, 0, 5);

        InventoryResponse expectedResponse = new InventoryResponse(
                saved.getId(), locationId, "WH-MAIN", productId, "SKU-001", "Laptop", 25, 0, 25, 5);

        Mockito.when(inventoryRepository.existsByProductIdAndLocationId(productId, locationId)).thenReturn(false);
        Mockito.when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        Mockito.when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        Mockito.when(inventoryRepository.save(Mockito.any(Inventory.class))).thenReturn(saved);
        Mockito.when(inventoryMapper.toResponse(saved)).thenReturn(expectedResponse);

        // When
        InventoryResponse result = inventoryService.createInventory(request);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResponse.id(), result.id());
        Assertions.assertEquals(25, result.quantity());
        Mockito.verify(inventoryRepository).save(Mockito.any(Inventory.class));
    }

    @Test
    void createInventory_ShouldThrow_WhenInventoryAlreadyExists()
    {
        // Given
        UUID locationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateInventoryRequest request = new CreateInventoryRequest(locationId, productId, 25, 5);

        Mockito.when(inventoryRepository.existsByProductIdAndLocationId(productId, locationId)).thenReturn(true);

        // When & Then
        ConflictException exception = Assertions.assertThrows(
                ConflictException.class,
                () -> inventoryService.createInventory(request)
        );
        Assertions.assertTrue(exception.getMessage().contains("already exists"));
        Mockito.verify(inventoryRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void addStock_ShouldIncreaseQuantityAndReturnResponse()
    {
        // Given
        UUID inventoryId = UUID.randomUUID();
        Product product = createProduct(UUID.randomUUID());
        Location location = createLocation(UUID.randomUUID());
        Inventory inventory = createInventory(inventoryId, location, product, 10, 0, 2);

        InventoryResponse expectedResponse = new InventoryResponse(
                inventoryId, location.getId(), "WH-MAIN", product.getId(), "SKU-001", "Laptop", 15, 0, 15, 2);

        Mockito.when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        Mockito.when(inventoryRepository.save(inventory)).thenReturn(inventory);
        Mockito.when(inventoryMapper.toResponse(inventory)).thenReturn(expectedResponse);

        // When
        InventoryResponse result = inventoryService.addStock(inventoryId, 5);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(15, inventory.getQuantity());
        Assertions.assertEquals(15, result.quantity());
        Mockito.verify(inventoryRepository).save(inventory);
    }

    @Test
    void deductStock_ShouldDecreaseQuantityAndReturnResponse()
    {
        // Given
        UUID inventoryId = UUID.randomUUID();
        Product product = createProduct(UUID.randomUUID());
        Location location = createLocation(UUID.randomUUID());
        Inventory inventory = createInventory(inventoryId, location, product, 10, 0, 2);

        InventoryResponse expectedResponse = new InventoryResponse(
                inventoryId, location.getId(), "WH-MAIN", product.getId(), "SKU-001", "Laptop", 7, 0, 7, 2);

        Mockito.when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        Mockito.when(inventoryRepository.save(inventory)).thenReturn(inventory);
        Mockito.when(inventoryMapper.toResponse(inventory)).thenReturn(expectedResponse);

        // When
        InventoryResponse result = inventoryService.deductStock(inventoryId, 3);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(7, inventory.getQuantity());
        Mockito.verify(inventoryRepository).save(inventory);
    }

    private Product createProduct(UUID id)
    {
        Product product = new Product("SKU-001", "Laptop", "Description", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Location createLocation(UUID id)
    {
        Location location = new Location("WH-MAIN", "Main Warehouse", LocationType.CENTRAL_WAREHOUSE, "Address");
        ReflectionTestUtils.setField(location, "id", id);
        return location;
    }

    private Inventory createInventory(UUID id, Location location, Product product, int quantity, int reserved, int minThreshold)
    {
        Inventory inventory = new Inventory(location, product, quantity, minThreshold);
        ReflectionTestUtils.setField(inventory, "id", id);
        if (reserved != 0)
        {
            ReflectionTestUtils.setField(inventory, "reservedQuantity", reserved);
        }
        return inventory;
    }
}