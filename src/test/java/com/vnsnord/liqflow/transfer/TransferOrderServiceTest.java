package com.vnsnord.liqflow.transfer;

import com.vnsnord.liqflow.common.exception.ConflictException;
import com.vnsnord.liqflow.common.exception.ProductNotFoundException;
import com.vnsnord.liqflow.common.exception.TransferOrderNotFoundException;
import com.vnsnord.liqflow.inventory.Inventory;
import com.vnsnord.liqflow.inventory.InventoryRepository;
import com.vnsnord.liqflow.location.Location;
import com.vnsnord.liqflow.location.LocationRepository;
import com.vnsnord.liqflow.location.LocationType;
import com.vnsnord.liqflow.product.Product;
import com.vnsnord.liqflow.product.ProductRepository;
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
import java.util.ArrayList;
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
    private TransferOrderReservationRepository reservationRepository;
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
        Assertions.assertThrows(ProductNotFoundException.class,
                () -> transferOrderService.addItem(orderId, request));
        Mockito.verify(transferOrderRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void submit_ShouldReserveSourceStockAndTransitionToSubmitted()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant now = Instant.now();
        Location source = createLocation(sourceId, "WH-MAIN", "Central Warehouse");
        Location target = createLocation(UUID.randomUUID(), "WH-HUB", "Regional Hub");
        Product product = createProduct(productId, "PROD-1", "Laptop");
        TransferOrder order = createTransferOrder(orderId, source, target, now);
        order.addItem(product, 2);

        Inventory sourceInventory = new Inventory(source, product, 10, 0);
        TransferOrderResponse dto = new TransferOrderResponse(
                orderId, "TR-10001", source.getId(), "WH-MAIN", target.getId(), "WH-HUB",
                TransferOrderStatus.SUBMITTED, now);

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(inventoryRepository.findByProductIdAndLocationIdForUpdate(productId, sourceId))
                .thenReturn(Optional.of(sourceInventory));
        Mockito.when(transferOrderRepository.save(order)).thenReturn(order);
        Mockito.when(transferOrderMapper.toResponse(order)).thenReturn(dto);

        // When
        TransferOrderResponse result = transferOrderService.submit(orderId);

        // Then
        Assertions.assertEquals(TransferOrderStatus.SUBMITTED, result.status());
        Assertions.assertEquals(TransferOrderStatus.SUBMITTED, order.getStatus());
        Assertions.assertEquals(2, sourceInventory.getReservedQuantity());
        Assertions.assertEquals(10, sourceInventory.getQuantity());
        Assertions.assertEquals(8, sourceInventory.getAvailableQuantity());
        Mockito.verify(reservationRepository).save(Mockito.any(TransferOrderReservation.class));
        Mockito.verify(transferOrderRepository).save(order);
    }

    @Test
    void submit_ShouldThrow_WhenSourceStockIsInsufficient()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Location source = createLocation(sourceId, "WH-MAIN", "Central Warehouse");
        Location target = createLocation(UUID.randomUUID(), "WH-HUB", "Regional Hub");
        Product product = createProduct(productId, "PROD-1", "Laptop");
        TransferOrder order = createTransferOrder(orderId, source, target, Instant.now());
        order.addItem(product, 9);

        Inventory sourceInventory = new Inventory(source, product, 5, 0);

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(inventoryRepository.findByProductIdAndLocationIdForUpdate(productId, sourceId))
                .thenReturn(Optional.of(sourceInventory));

        // When & Then
        Assertions.assertThrows(IllegalStateException.class, () -> transferOrderService.submit(orderId));
        Mockito.verify(reservationRepository, Mockito.never()).save(Mockito.any());
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
        UUID itemId = UUID.randomUUID();

        Location source = createLocation(sourceId, "WH-MAIN", "Central Warehouse");
        Location target = createLocation(targetId, "WH-HUB", "Regional Hub");
        Product product = createProduct(productId, "PROD-1", "Laptop");
        TransferOrder order = createTransferOrder(orderId, source, target, now);
        order.addItem(product, 5);
        TransferOrderItem item = order.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", itemId);
        ReflectionTestUtils.setField(order, "status", TransferOrderStatus.IN_TRANSIT);

        TransferOrderReservation reservation = new TransferOrderReservation(order, item, product, source, 5);

        Inventory sourceInventory = new Inventory(source, product, 10, 0);
        sourceInventory.reserveStock(5);
        Inventory targetInventory = new Inventory(target, product, 1, 0);

        TransferOrderResponse dto = new TransferOrderResponse(
                orderId, "TR-10001", sourceId, "WH-MAIN", targetId, "WH-HUB",
                TransferOrderStatus.COMPLETED, now);

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(reservationRepository.findByTransferOrderIdForUpdate(orderId))
                .thenReturn(List.of(reservation));
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
        Assertions.assertEquals(0, sourceInventory.getReservedQuantity());
        Assertions.assertEquals(6, targetInventory.getQuantity());
        Assertions.assertEquals(ReservationStatus.CONSUMED, reservation.getStatus());
        Mockito.verify(transferOrderRepository).save(order);
    }

    @Test
    void complete_ShouldThrow_WhenAnItemHasNoReservation()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Location source = createLocation(sourceId, "WH-MAIN", "Central Warehouse");
        Location target = createLocation(targetId, "WH-HUB", "Regional Hub");
        Product product = createProduct(productId, "PROD-1", "Laptop");
        TransferOrder order = createTransferOrder(orderId, source, target, Instant.now());
        order.addItem(product, 5);
        ReflectionTestUtils.setField(order.getItems().getFirst(), "id", UUID.randomUUID());
        ReflectionTestUtils.setField(order, "status", TransferOrderStatus.IN_TRANSIT);

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(reservationRepository.findByTransferOrderIdForUpdate(orderId)).thenReturn(List.of());
        Mockito.when(inventoryRepository.findByProductIdAndLocationIdForUpdate(productId, sourceId))
                .thenReturn(Optional.of(new Inventory(source, product, 10, 0)));
        Mockito.when(inventoryRepository.findByProductIdAndLocationIdForUpdate(productId, targetId))
                .thenReturn(Optional.of(new Inventory(target, product, 1, 0)));

        // When & Then
        Assertions.assertThrows(IllegalStateException.class, () -> transferOrderService.complete(orderId));
        Mockito.verify(transferOrderRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void complete_ShouldNeverTouchStockReservedForAnotherOrder()
    {
        // Given: order A holds 5 units, order B holds 3 of the same product at
        // the same source. Only A is being completed.
        UUID orderId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Location source = createLocation(sourceId, "WH-MAIN", "Central Warehouse");
        Location target = createLocation(targetId, "WH-HUB", "Regional Hub");
        Product product = createProduct(productId, "PROD-1", "Laptop");
        TransferOrder order = createTransferOrder(orderId, source, target, Instant.now());
        order.addItem(product, 5);
        TransferOrderItem item = order.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", itemId);
        ReflectionTestUtils.setField(order, "status", TransferOrderStatus.IN_TRANSIT);

        TransferOrderReservation reservation = new TransferOrderReservation(order, item, product, source, 5);

        Inventory sourceInventory = new Inventory(source, product, 10, 0);
        sourceInventory.reserveStock(5);
        sourceInventory.reserveStock(3);
        Inventory targetInventory = new Inventory(target, product, 1, 0);

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(reservationRepository.findByTransferOrderIdForUpdate(orderId))
                .thenReturn(List.of(reservation));
        Mockito.when(inventoryRepository.findByProductIdAndLocationIdForUpdate(productId, sourceId))
                .thenReturn(Optional.of(sourceInventory));
        Mockito.when(inventoryRepository.findByProductIdAndLocationIdForUpdate(productId, targetId))
                .thenReturn(Optional.of(targetInventory));
        Mockito.when(transferOrderRepository.save(order)).thenReturn(order);
        Mockito.when(transferOrderMapper.toResponse(order))
                .thenReturn(new TransferOrderResponse(orderId, "TR-10001", sourceId, "WH-MAIN",
                        targetId, "WH-HUB", TransferOrderStatus.COMPLETED, Instant.now()));

        // When
        transferOrderService.complete(orderId);

        // Then: order B's 3 units are still reserved and still physically present.
        Assertions.assertEquals(5, sourceInventory.getQuantity());
        Assertions.assertEquals(3, sourceInventory.getReservedQuantity());
        Assertions.assertEquals(2, sourceInventory.getAvailableQuantity());
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
        Mockito.when(reservationRepository.findByTransferOrderIdAndStatus(orderId, ReservationStatus.HELD))
                .thenReturn(List.of());
        Mockito.when(transferOrderRepository.save(order)).thenReturn(order);
        Mockito.when(transferOrderMapper.toResponse(order)).thenReturn(dto);

        // When
        TransferOrderResponse result = transferOrderService.cancel(orderId);

        // Then
        Assertions.assertEquals(TransferOrderStatus.CANCELLED, result.status());
        Mockito.verify(transferOrderRepository).save(order);
    }

    @Test
    void cancel_ShouldReleaseHeldReservations()
    {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Location source = createLocation(sourceId, "WH-MAIN", "Central Warehouse");
        Location target = createLocation(UUID.randomUUID(), "WH-HUB", "Regional Hub");
        Product product = createProduct(productId, "PROD-1", "Laptop");
        TransferOrder order = createTransferOrder(orderId, source, target, Instant.now());
        order.addItem(product, 4);
        TransferOrderItem item = order.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", itemId);

        TransferOrderReservation reservation = new TransferOrderReservation(order, item, product, source, 4);

        Inventory sourceInventory = new Inventory(source, product, 10, 0);
        sourceInventory.reserveStock(4);

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(reservationRepository.findByTransferOrderIdAndStatus(orderId, ReservationStatus.HELD))
                .thenReturn(List.of(reservation));
        Mockito.when(inventoryRepository.findByProductIdAndLocationIdForUpdate(productId, sourceId))
                .thenReturn(Optional.of(sourceInventory));
        Mockito.when(transferOrderRepository.save(order)).thenReturn(order);
        Mockito.when(transferOrderMapper.toResponse(order))
                .thenReturn(new TransferOrderResponse(orderId, "TR-10001", sourceId, "WH-MAIN",
                        target.getId(), "WH-HUB", TransferOrderStatus.CANCELLED, Instant.now()));

        // When
        transferOrderService.cancel(orderId);

        // Then: the held units are back in the available quantity.
        Assertions.assertEquals(10, sourceInventory.getQuantity());
        Assertions.assertEquals(0, sourceInventory.getReservedQuantity());
        Assertions.assertEquals(10, sourceInventory.getAvailableQuantity());
        Assertions.assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
    }

    @Test
    void complete_ShouldLockInventoriesInTheSameOrderRegardlessOfItemOrder()
    {
        // Given: two orders covering the same two locations and two products,
        // but listing the items in opposite order. Deterministic locking is
        // what stops these two from deadlocking against each other, so the
        // property under test is that both produce an identical lock sequence.
        UUID lowLocationId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID highLocationId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UUID lowProductId = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        UUID highProductId = UUID.fromString("ffffffff-ffff-ffff-ffff-fffffffffffa");

        Location source = createLocation(lowLocationId, "WH-A", "Source A");
        Location target = createLocation(highLocationId, "WH-B", "Target B");
        Product lowProduct = createProduct(lowProductId, "PROD-LOW", "Low");
        Product highProduct = createProduct(highProductId, "PROD-HIGH", "High");

        List<UUID> forwardSequence = lockSequenceFor(orderWithItemsInOrder(source, target, highProduct, lowProduct));
        List<UUID> reversedSequence = lockSequenceFor(orderWithItemsInOrder(source, target, lowProduct, highProduct));

        // Then
        Assertions.assertEquals(forwardSequence, reversedSequence);
        Assertions.assertEquals(4, forwardSequence.size());
        Assertions.assertEquals(2, forwardSequence.stream().distinct().count());
    }

    /**
     * Runs completion for an order carrying the given items and returns the
     * order in which inventory locks were acquired. Completion itself is
     * expected to fail, since no reservations were set up; only the lock
     * sequence matters here.
     */
    private List<UUID> lockSequenceFor(TransferOrder order)
    {
        UUID orderId = order.getId();
        List<UUID> lockSequence = new ArrayList<>();

        Mockito.when(transferOrderRepository.findWithDetailsById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(reservationRepository.findByTransferOrderIdForUpdate(orderId)).thenReturn(List.of());
        Mockito.doAnswer(invocation ->
                {
                    UUID locationId = invocation.getArgument(1);
                    lockSequence.add(locationId);
                    return Optional.of(new Inventory(
                            locationId.equals(order.getSourceLocation().getId())
                                    ? order.getSourceLocation() : order.getTargetLocation(),
                            new Product("SKU", "Name", "Desc", BigDecimal.ONE), 10, 0));
                }).when(inventoryRepository)
                .findByProductIdAndLocationIdForUpdate(Mockito.any(), Mockito.any());

        Assertions.assertThrows(IllegalStateException.class, () -> transferOrderService.complete(orderId));
        return lockSequence;
    }

    private TransferOrder orderWithItemsInOrder(Location source, Location target, Product... products)
    {
        TransferOrder order = createTransferOrder(UUID.randomUUID(), source, target, Instant.now());
        for (Product product : products)
        {
            order.addItem(product, 1);
            ReflectionTestUtils.setField(order.getItems().getLast(), "id", UUID.randomUUID());
        }
        ReflectionTestUtils.setField(order, "status", TransferOrderStatus.IN_TRANSIT);
        return order;
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
