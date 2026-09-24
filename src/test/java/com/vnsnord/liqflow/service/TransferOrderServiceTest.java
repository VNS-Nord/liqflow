package com.vnsnord.liqflow.service;

import com.vnsnord.liqflow.domain.entity.Inventory;
import com.vnsnord.liqflow.domain.entity.Location;
import com.vnsnord.liqflow.domain.entity.Product;
import com.vnsnord.liqflow.domain.entity.TransferOrder;
import com.vnsnord.liqflow.domain.enums.LocationType;
import com.vnsnord.liqflow.domain.enums.TransferOrderStatus;
import com.vnsnord.liqflow.dto.CreateTransferOrderRequest;
import com.vnsnord.liqflow.dto.TransferOrderDetailResponse;
import com.vnsnord.liqflow.dto.TransferOrderItemRequest;
import com.vnsnord.liqflow.dto.TransferOrderItemResponse;
import com.vnsnord.liqflow.dto.TransferOrderResponse;
import com.vnsnord.liqflow.exception.ConflictException;
import com.vnsnord.liqflow.exception.TransferOrderNotFoundException;
import com.vnsnord.liqflow.infrastructure.persistence.InventoryRepository;
import com.vnsnord.liqflow.infrastructure.persistence.LocationRepository;
import com.vnsnord.liqflow.infrastructure.persistence.ProductRepository;
import com.vnsnord.liqflow.infrastructure.persistence.TransferOrderRepository;
import com.vnsnord.liqflow.service.mapper.TransferOrderMapper;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class TransferOrderServiceTest
{
    @Mock
    private TransferOrderRepository transferOrderRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private TransferOrderMapper transferOrderMapper;
    @InjectMocks
    private TransferOrderService transferOrderService;

    @Test
    void getTransferOrderById_ShouldReturnTransferOrderResponseDto_WhenOrderExists()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID sourceLocationId = UUID.randomUUID();
        UUID targetLocationId = UUID.randomUUID();
        Instant now = Instant.now();

        Location sourceLocation = createLocation(sourceLocationId, "WH-MAIN", "Central Warehouse");
        Location targetLocation = createLocation(targetLocationId, "WH-HUB", "Regional Hub");
        TransferOrder order = createTransferOrder(orderId, sourceLocation, targetLocation, now);

        TransferOrderResponse expectedDto = new TransferOrderResponse(
                orderId,
                "TR-10001",
                sourceLocationId,
                "WH-MAIN",
                targetLocationId,
                "WH-HUB",
                TransferOrderStatus.DRAFT,
                now
        );

        Mockito.when(transferOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(transferOrderMapper.toResponse(order)).thenReturn(expectedDto);

        // When
        TransferOrderResponse actualDto = transferOrderService.getTransferOrderById(orderId);

        // Then
        Assertions.assertNotNull(actualDto);
        Assertions.assertEquals(expectedDto.id(), actualDto.id());
        Assertions.assertEquals(expectedDto.orderNumber(), actualDto.orderNumber());
        Assertions.assertEquals(expectedDto.sourceLocationId(), actualDto.sourceLocationId());
        Assertions.assertEquals(expectedDto.targetLocationId(), actualDto.targetLocationId());
        Assertions.assertEquals(expectedDto.status(), actualDto.status());

        Mockito.verify(transferOrderRepository).findById(orderId);
        Mockito.verify(transferOrderMapper).toResponse(order);
    }

    @Test
    void getTransferOrderById_ShouldThrowTransferOrderNotFoundException_WhenOrderDoesNotExist()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        Mockito.when(transferOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        TransferOrderNotFoundException exception = Assertions.assertThrows(
                TransferOrderNotFoundException.class,
                () -> transferOrderService.getTransferOrderById(orderId)
        );

        Assertions.assertEquals("Transfer order not found with id: '" + orderId + "'", exception.getMessage());

        Mockito.verify(transferOrderRepository).findById(orderId);
        Mockito.verifyNoInteractions(transferOrderMapper);
    }

    @Test
    void getAllTransferOrders_ShouldReturnPagedTransferOrderResponseDtos()
    {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        UUID orderId = UUID.randomUUID();
        UUID sourceLocationId = UUID.randomUUID();
        UUID targetLocationId = UUID.randomUUID();
        Instant now = Instant.now();

        Location sourceLocation = createLocation(sourceLocationId, "WH-MAIN", "Central Warehouse");
        Location targetLocation = createLocation(targetLocationId, "WH-HUB", "Regional Hub");
        TransferOrder order = createTransferOrder(orderId, sourceLocation, targetLocation, now);

        TransferOrderResponse dto = new TransferOrderResponse(
                orderId,
                "TR-10001",
                sourceLocationId,
                "WH-MAIN",
                targetLocationId,
                "WH-HUB",
                TransferOrderStatus.DRAFT,
                now
        );

        Page<TransferOrder> page = new PageImpl<>(List.of(order), pageable, 1);

        Mockito.when(transferOrderRepository.findAllWithLocations(pageable)).thenReturn(page);
        Mockito.when(transferOrderMapper.toResponse(order)).thenReturn(dto);

        // When
        Page<TransferOrderResponse> result = transferOrderService.getAllTransferOrders(pageable);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals(1, result.getContent().size());
        Assertions.assertEquals(orderId, result.getContent().getFirst().id());

        Mockito.verify(transferOrderRepository).findAllWithLocations(pageable);
        Mockito.verify(transferOrderMapper).toResponse(order);
    }

    @Test
    void getTransferOrderDetailById_ShouldReturnDetailResponseWithItems()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();

        Location source = createLocation(sourceId, "WH-MAIN", "Central Warehouse");
        Location target = createLocation(targetId, "WH-HUB", "Regional Hub");
        Product product = createProduct(productId, "PROD-1", "Laptop");
        TransferOrder order = createTransferOrder(orderId, source, target, now);
        order.addItem(product, 3);

        TransferOrderItemResponse itemResponse = new TransferOrderItemResponse(
                order.getItems().getFirst().getId(), productId, "PROD-1", "Laptop", 3);
        TransferOrderDetailResponse expected = new TransferOrderDetailResponse(
                orderId, "TR-10001", sourceId, "WH-MAIN", targetId, "WH-HUB",
                TransferOrderStatus.DRAFT, now, List.of(itemResponse));

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(transferOrderMapper.toDetailResponse(order)).thenReturn(expected);

        // When
        TransferOrderDetailResponse result = transferOrderService.getTransferOrderDetailById(orderId);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(orderId, result.id());
        Assertions.assertEquals(1, result.items().size());
        Assertions.assertEquals("PROD-1", result.items().getFirst().productSku());
        Mockito.verify(transferOrderRepository).findWithDetailsById(orderId);
    }

    @Test
    void getTransferOrderDetailById_ShouldThrowTransferOrderNotFoundException_WhenOrderDoesNotExist()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.empty());

        // When & Then
        Assertions.assertThrows(TransferOrderNotFoundException.class,
                () -> transferOrderService.getTransferOrderDetailById(orderId));
    }

    @Test
    void createTransferOrder_ShouldSaveAndReturnResponse()
    {
        // Given
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Instant now = Instant.now();

        Location source = createLocation(sourceId, "WH-MAIN", "Central Warehouse");
        Location target = createLocation(targetId, "WH-HUB", "Regional Hub");
        TransferOrder order = createTransferOrder(UUID.randomUUID(), source, target, now);
        TransferOrderResponse dto = new TransferOrderResponse(
                order.getId(), "TR-200", sourceId, "WH-MAIN", targetId, "WH-HUB",
                TransferOrderStatus.DRAFT, now);

        CreateTransferOrderRequest request = new CreateTransferOrderRequest("TR-200", sourceId, targetId);

        Mockito.when(transferOrderRepository.existsByOrderNumber("TR-200")).thenReturn(false);
        Mockito.when(locationRepository.findById(sourceId)).thenReturn(Optional.of(source));
        Mockito.when(locationRepository.findById(targetId)).thenReturn(Optional.of(target));
        Mockito.when(transferOrderRepository.save(Mockito.any(TransferOrder.class))).thenReturn(order);
        Mockito.when(transferOrderMapper.toResponse(order)).thenReturn(dto);

        // When
        TransferOrderResponse result = transferOrderService.createTransferOrder(request);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("TR-200", result.orderNumber());
        Mockito.verify(transferOrderRepository).save(Mockito.any(TransferOrder.class));
    }

    @Test
    void createTransferOrder_ShouldThrow_WhenOrderNumberAlreadyExists()
    {
        // Given
        CreateTransferOrderRequest request = new CreateTransferOrderRequest(
                "TR-200", UUID.randomUUID(), UUID.randomUUID());
        Mockito.when(transferOrderRepository.existsByOrderNumber("TR-200")).thenReturn(true);

        // When & Then
        Assertions.assertThrows(ConflictException.class,
                () -> transferOrderService.createTransferOrder(request));
        Mockito.verify(transferOrderRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void addItem_ShouldAddItemToDraftOrderAndReturnDetail()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();

        Location source = createLocation(UUID.randomUUID(), "WH-MAIN", "Central Warehouse");
        Location target = createLocation(UUID.randomUUID(), "WH-HUB", "Regional Hub");
        TransferOrder order = createTransferOrder(orderId, source, target, now);
        Product product = createProduct(productId, "PROD-1", "Laptop");

        TransferOrderItemRequest request = new TransferOrderItemRequest(productId, 5);
        TransferOrderDetailResponse dto = new TransferOrderDetailResponse(
                orderId, "TR-10001", source.getId(), "WH-MAIN", target.getId(), "WH-HUB",
                TransferOrderStatus.DRAFT, now, List.of(
                new TransferOrderItemResponse(UUID.randomUUID(), productId, "PROD-1", "Laptop", 5)));

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        Mockito.when(transferOrderRepository.save(order)).thenReturn(order);
        Mockito.when(transferOrderMapper.toDetailResponse(order)).thenReturn(dto);

        // When
        TransferOrderDetailResponse result = transferOrderService.addItem(orderId, request);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, order.getItems().size());
        Assertions.assertEquals(5, order.getItems().getFirst().getQuantity());
        Mockito.verify(transferOrderRepository).save(order);
    }

    @Test
    void addItem_ShouldThrowProductNotFound_WhenProductDoesNotExist()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();

        Location source = createLocation(UUID.randomUUID(), "WH-MAIN", "Central Warehouse");
        Location target = createLocation(UUID.randomUUID(), "WH-HUB", "Regional Hub");
        TransferOrder order = createTransferOrder(orderId, source, target, now);

        TransferOrderItemRequest request = new TransferOrderItemRequest(productId, 5);

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // When & Then
        Assertions.assertThrows(com.vnsnord.liqflow.exception.ProductNotFoundException.class,
                () -> transferOrderService.addItem(orderId, request));
        Mockito.verify(transferOrderRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void submit_ShouldTransitionToSubmitted()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();
        Location source = createLocation(UUID.randomUUID(), "WH-MAIN", "Central Warehouse");
        Location target = createLocation(UUID.randomUUID(), "WH-HUB", "Regional Hub");
        TransferOrder order = createTransferOrder(orderId, source, target, now);
        order.addItem(createProduct(UUID.randomUUID(), "PROD-1", "Laptop"), 2);

        TransferOrderResponse dto = new TransferOrderResponse(
                orderId, "TR-10001", source.getId(), "WH-MAIN", target.getId(), "WH-HUB",
                TransferOrderStatus.SUBMITTED, now);

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(transferOrderRepository.save(order)).thenReturn(order);
        Mockito.when(transferOrderMapper.toResponse(order)).thenReturn(dto);

        // When
        TransferOrderResponse result = transferOrderService.submit(orderId);

        // Then
        Assertions.assertEquals(TransferOrderStatus.SUBMITTED, result.status());
        Assertions.assertEquals(TransferOrderStatus.SUBMITTED, order.getStatus());
        Mockito.verify(transferOrderRepository).save(order);
    }

    @Test
    void complete_ShouldMoveStockAndTransitionToCompleted()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Location source = createLocation(sourceId, "WH-MAIN", "Central Warehouse");
        Location target = createLocation(targetId, "WH-HUB", "Regional Hub");
        Product product = createProduct(productId, "PROD-1", "Laptop");
        TransferOrder order = createTransferOrder(orderId, source, target, now);
        order.addItem(product, 5);
        ReflectionTestUtils.setField(order, "status", TransferOrderStatus.IN_TRANSIT);

        Inventory sourceInventory = new Inventory(source, product, 10, 0);
        Inventory targetInventory = new Inventory(target, product, 1, 0);

        TransferOrderResponse dto = new TransferOrderResponse(
                orderId, "TR-10001", sourceId, "WH-MAIN", targetId, "WH-HUB",
                TransferOrderStatus.COMPLETED, now);

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(inventoryRepository.findByProductIdAndLocationIdForUpdate(productId, sourceId))
                .thenReturn(Optional.of(sourceInventory));
        Mockito.when(inventoryRepository.findByProductIdAndLocationIdForUpdate(productId, targetId))
                .thenReturn(Optional.of(targetInventory));
        Mockito.when(transferOrderRepository.save(order)).thenReturn(order);
        Mockito.when(transferOrderMapper.toResponse(order)).thenReturn(dto);

        // When
        TransferOrderResponse result = transferOrderService.complete(orderId);

        // Then
        Assertions.assertEquals(TransferOrderStatus.COMPLETED, result.status());
        Assertions.assertEquals(5, sourceInventory.getQuantity());
        Assertions.assertEquals(6, targetInventory.getQuantity());
        Mockito.verify(transferOrderRepository).save(order);
    }

    @Test
    void cancel_ShouldTransitionToCancelled()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();
        Location source = createLocation(UUID.randomUUID(), "WH-MAIN", "Central Warehouse");
        Location target = createLocation(UUID.randomUUID(), "WH-HUB", "Regional Hub");
        TransferOrder order = createTransferOrder(orderId, source, target, now);

        TransferOrderResponse dto = new TransferOrderResponse(
                orderId, "TR-10001", source.getId(), "WH-MAIN", target.getId(), "WH-HUB",
                TransferOrderStatus.CANCELLED, now);

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(transferOrderRepository.save(order)).thenReturn(order);
        Mockito.when(transferOrderMapper.toResponse(order)).thenReturn(dto);

        // When
        TransferOrderResponse result = transferOrderService.cancel(orderId);

        // Then
        Assertions.assertEquals(TransferOrderStatus.CANCELLED, result.status());
        Mockito.verify(transferOrderRepository).save(order);
    }

    private Product createProduct(UUID id, String sku, String name)
    {
        Product product = new Product(sku, name, "Description", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Location createLocation(UUID id, String code, String name)
    {
        Location location = new Location(code, name, LocationType.CENTRAL_WAREHOUSE, "Address");
        ReflectionTestUtils.setField(location, "id", id);
        return location;
    }

    private TransferOrder createTransferOrder(UUID id, Location source, Location target, Instant createdAt)
    {
        TransferOrder order = new TransferOrder("TR-10001", source, target);
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "status", TransferOrderStatus.DRAFT);
        ReflectionTestUtils.setField(order, "createdAt", createdAt);
        return order;
    }
}
