package com.vnsnord.liqflow.integration;

import com.vnsnord.liqflow.domain.entity.Inventory;
import com.vnsnord.liqflow.domain.enums.LocationType;
import com.vnsnord.liqflow.domain.enums.TransferOrderStatus;
import com.vnsnord.liqflow.dto.request.*;
import com.vnsnord.liqflow.dto.response.InventoryResponse;
import com.vnsnord.liqflow.dto.response.LocationResponse;
import com.vnsnord.liqflow.dto.response.ProductResponse;
import com.vnsnord.liqflow.dto.response.TransferOrderResponse;
import com.vnsnord.liqflow.infrastructure.persistence.InventoryRepository;
import com.vnsnord.liqflow.infrastructure.persistence.LocationRepository;
import com.vnsnord.liqflow.infrastructure.persistence.ProductRepository;
import com.vnsnord.liqflow.infrastructure.persistence.TransferOrderRepository;
import com.vnsnord.liqflow.service.InventoryService;
import com.vnsnord.liqflow.service.LocationService;
import com.vnsnord.liqflow.service.ProductService;
import com.vnsnord.liqflow.service.TransferOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Integration tests that exercise the real persistence layer and the stock
 * movement flows against a running PostgreSQL instance.
 *
 * <p>These tests deliberately do <em>not</em> run inside a test transaction.
 * Each {@code @Transactional} service call commits on its own, so a regression
 * where a read-only service transaction silently skipped database writes
 * becomes a hard failure instead of passing inside a rollback-only test
 * transaction.</p>
 *
 * <p>The tests require the database configured in
 * {@code src/test/resources/application.yaml} (the separate {@code liqflow_test}
 * database, so development data is never touched) and clean up all records they
 * create.</p>
 */
@SpringBootTest
public class InventoryFlowIntegrationTest
{
    private static final int CONCURRENT_ROUNDS = 20;
    private final List<UUID> productIds = new ArrayList<>();
    private final List<UUID> locationIds = new ArrayList<>();
    private final List<UUID> inventoryIds = new ArrayList<>();
    private final List<UUID> orderIds = new ArrayList<>();
    @Autowired
    private ProductService productService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private TransferOrderService transferOrderService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private TransferOrderRepository transferOrderRepository;

    @AfterEach
    void cleanUp()
    {
        for (UUID id : orderIds)
        {
            transferOrderRepository.deleteById(id);
        }
        for (UUID id : inventoryIds)
        {
            inventoryRepository.deleteById(id);
        }
        for (UUID id : locationIds)
        {
            locationRepository.deleteById(id);
        }
        for (UUID id : productIds)
        {
            productRepository.deleteById(id);
        }
        orderIds.clear();
        inventoryIds.clear();
        locationIds.clear();
        productIds.clear();
    }

    @Test
    void createProduct_ShouldCommitARowToTheDatabase()
    {
        // Given
        String sku = "IT-" + UUID.randomUUID().toString().substring(0, 8);

        // When
        UUID productId = createProduct(sku);

        // Then: the row must be visible to a fresh committed query. This is the
        // regression test for the read-only-service-transaction flush bug where
        // rows were silently never written to the database.
        Assertions.assertTrue(productRepository.existsBySku(sku));
        Assertions.assertTrue(productRepository.findById(productId).isPresent());
    }

    @Test
    void deductStock_ShouldNotConsumeReservedUnitsInTheRealDatabase()
    {
        // Given
        UUID productId = createProduct("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID locationId = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        createInventory(locationId, productId, 10, 4);

        Inventory inventory = inventoryRepository.findByProductIdAndLocationId(productId, locationId)
                .orElseThrow();

        // Reserve 4 units, leaving 6 unreserved (available) units.
        inventory.reserveStock(4);
        inventoryRepository.saveAndFlush(inventory);

        // Reload as a real caller would between two committed transactions.
        Inventory withReservation = inventoryRepository.findByProductIdAndLocationId(productId, locationId)
                .orElseThrow();

        // When: try to deduct 8 units. There are 10 physical units, so the old
        // implementation happily took the 4 reserved ones too. A correction must
        // not be able to strand a reservation, so this now fails instead.
        Assertions.assertThrows(IllegalStateException.class, () -> withReservation.deductStock(8));

        // Then: the reservation and the physical stock are both untouched.
        Inventory reloaded = inventoryRepository.findByProductIdAndLocationId(productId, locationId)
                .orElseThrow();
        Assertions.assertEquals(10, reloaded.getQuantity());
        Assertions.assertEquals(4, reloaded.getReservedQuantity());
        Assertions.assertEquals(6, reloaded.getAvailableQuantity());
    }

    @Test
    void completeTransferOrder_ShouldMoveStockInTheRealDatabase()
    {
        // Given
        UUID productId = createProduct("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID sourceId = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID targetId = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        createInventory(sourceId, productId, 10, 4);
        createInventory(targetId, productId, 3, 4);

        UUID orderId = createTransferOrder(
                "IT-TO-" + UUID.randomUUID().toString().substring(0, 8), sourceId, targetId);

        transferOrderService.addItem(orderId, new TransferOrderItemRequest(productId, 4));
        transferOrderService.submit(orderId);
        transferOrderService.markInTransit(orderId);

        // When
        TransferOrderResponse completed = transferOrderService.complete(orderId);

        // Then
        Assertions.assertEquals(TransferOrderStatus.COMPLETED, completed.status());

        Inventory source = inventoryRepository.findByProductIdAndLocationId(productId, sourceId)
                .orElseThrow();
        Inventory target = inventoryRepository.findByProductIdAndLocationId(productId, targetId)
                .orElseThrow();

        Assertions.assertEquals(6, source.getQuantity());
        Assertions.assertEquals(6, source.getAvailableQuantity());
        Assertions.assertTrue(source.getAvailableQuantity() >= 0);

        Assertions.assertEquals(7, target.getQuantity());
        Assertions.assertEquals(7, target.getAvailableQuantity());
        Assertions.assertTrue(target.getAvailableQuantity() >= 0);
    }

    @Test
    void completingOneOrder_ShouldNeverConsumeAnotherOrdersReservation()
    {
        // Given: two orders competing for the same source stock. Order A wants
        // 6 of 10 units, order B wants the remaining 4. Submitting both must
        // succeed, which only works if each holds its own reservation.
        UUID productId = createProduct("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID sourceId = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID targetId = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        createInventory(sourceId, productId, 10, 0);
        createInventory(targetId, productId, 0, 0);

        UUID orderA = createTransferOrder(
                "IT-A-" + UUID.randomUUID().toString().substring(0, 8), sourceId, targetId);
        UUID orderB = createTransferOrder(
                "IT-B-" + UUID.randomUUID().toString().substring(0, 8), sourceId, targetId);

        transferOrderService.addItem(orderA, new TransferOrderItemRequest(productId, 6));
        transferOrderService.addItem(orderB, new TransferOrderItemRequest(productId, 4));
        transferOrderService.submit(orderA);
        transferOrderService.submit(orderB);

        Inventory afterBothSubmitted = inventoryRepository.findByProductIdAndLocationId(productId, sourceId)
                .orElseThrow();
        Assertions.assertEquals(10, afterBothSubmitted.getQuantity());
        Assertions.assertEquals(10, afterBothSubmitted.getReservedQuantity());
        Assertions.assertEquals(0, afterBothSubmitted.getAvailableQuantity());

        // When: only order A is completed.
        transferOrderService.markInTransit(orderA);
        transferOrderService.complete(orderA);

        // Then: order B's 4 units are still reserved, and still physically present.
        Inventory afterCompletingA = inventoryRepository.findByProductIdAndLocationId(productId, sourceId)
                .orElseThrow();
        Assertions.assertEquals(4, afterCompletingA.getQuantity());
        Assertions.assertEquals(4, afterCompletingA.getReservedQuantity());
        Assertions.assertEquals(0, afterCompletingA.getAvailableQuantity());

        // And order B can still complete on the strength of its own reservation.
        transferOrderService.markInTransit(orderB);
        transferOrderService.complete(orderB);

        Inventory afterCompletingB = inventoryRepository.findByProductIdAndLocationId(productId, sourceId)
                .orElseThrow();
        Assertions.assertEquals(0, afterCompletingB.getQuantity());
        Assertions.assertEquals(0, afterCompletingB.getReservedQuantity());
    }

    @Test
    void cancel_ShouldReleaseReservedStockBackToTheSource()
    {
        // Given
        UUID productId = createProduct("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID sourceId = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID targetId = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        createInventory(sourceId, productId, 10, 0);

        UUID orderId = createTransferOrder(
                "IT-C-" + UUID.randomUUID().toString().substring(0, 8), sourceId, targetId);
        transferOrderService.addItem(orderId, new TransferOrderItemRequest(productId, 7));
        transferOrderService.submit(orderId);

        Inventory held = inventoryRepository.findByProductIdAndLocationId(productId, sourceId).orElseThrow();
        Assertions.assertEquals(7, held.getReservedQuantity());
        Assertions.assertEquals(3, held.getAvailableQuantity());

        // When
        transferOrderService.cancel(orderId);

        // Then
        Inventory released = inventoryRepository.findByProductIdAndLocationId(productId, sourceId).orElseThrow();
        Assertions.assertEquals(10, released.getQuantity());
        Assertions.assertEquals(0, released.getReservedQuantity());
        Assertions.assertEquals(10, released.getAvailableQuantity());
    }

    @Test
    void submit_ShouldFail_WhenSourceStockIsInsufficient()
    {
        // Given
        UUID productId = createProduct("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID sourceId = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID targetId = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        createInventory(sourceId, productId, 3, 0);

        UUID orderId = createTransferOrder(
                "IT-S-" + UUID.randomUUID().toString().substring(0, 8), sourceId, targetId);
        transferOrderService.addItem(orderId, new TransferOrderItemRequest(productId, 9));

        // When & Then
        Assertions.assertThrows(IllegalStateException.class, () -> transferOrderService.submit(orderId));
    }

    @Test
    void concurrentTransfersInOppositeDirections_ShouldNotDeadlock() throws Exception
    {
        UUID productId = createProduct("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID locationA = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        UUID locationB = createLocation("IT-" + UUID.randomUUID().toString().substring(0, 8));
        int initialQuantity = 1000;
        createInventory(locationA, productId, initialQuantity, 0);
        createInventory(locationB, productId, initialQuantity, 0);

        // The two orders move different amounts, so the final quantities depend on
        // both movements having been applied rather than cancelling each other out.
        int aToBQuantity = 10;
        int bToAQuantity = 25;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
        {
            for (int round = 0; round < CONCURRENT_ROUNDS; round++)
            {
                UUID aToB = createTransferOrder(
                        "IT-AB-" + UUID.randomUUID().toString().substring(0, 8), locationA, locationB);
                UUID bToA = createTransferOrder(
                        "IT-BA-" + UUID.randomUUID().toString().substring(0, 8), locationB, locationA);

                transferOrderService.addItem(aToB, new TransferOrderItemRequest(productId, aToBQuantity));
                transferOrderService.addItem(bToA, new TransferOrderItemRequest(productId, bToAQuantity));
                transferOrderService.submit(aToB);
                transferOrderService.submit(bToA);
                transferOrderService.markInTransit(aToB);
                transferOrderService.markInTransit(bToA);

                CountDownLatch startTogether = new CountDownLatch(1);
                Future<TransferOrderResponse> aToBCompletion = executor.submit(() ->
                {
                    startTogether.await();
                    return transferOrderService.complete(aToB);
                });
                Future<TransferOrderResponse> bToACompletion = executor.submit(() ->
                {
                    startTogether.await();
                    return transferOrderService.complete(bToA);
                });
                startTogether.countDown();
                Assertions.assertEquals(TransferOrderStatus.COMPLETED,
                        aToBCompletion.get(15, TimeUnit.SECONDS).status(),
                        "round " + round + ": the A-to-B transfer did not complete");
                Assertions.assertEquals(TransferOrderStatus.COMPLETED,
                        bToACompletion.get(15, TimeUnit.SECONDS).status(),
                        "round " + round + ": the B-to-A transfer did not complete");
            }
        } finally
        {
            executor.shutdownNow();
        }

        Inventory locationAInventory = inventoryRepository
                .findByProductIdAndLocationId(productId, locationA)
                .orElseThrow();
        Inventory locationBInventory = inventoryRepository
                .findByProductIdAndLocationId(productId, locationB)
                .orElseThrow();

        Assertions.assertEquals(0, locationAInventory.getReservedQuantity());
        Assertions.assertEquals(0, locationBInventory.getReservedQuantity());
        Assertions.assertEquals(2 * initialQuantity,
                locationAInventory.getQuantity() + locationBInventory.getQuantity());
    }

    private UUID createProduct(String sku)
    {
        ProductResponse response = productService.createProduct(
                new CreateProductRequest(sku, "Integration Product", "Created by integration test",
                        new BigDecimal("9.99")));
        productIds.add(response.id());
        return response.id();
    }

    private UUID createLocation(String code)
    {
        LocationResponse response = locationService.createLocation(
                new CreateLocationRequest(code, "Integration Location", LocationType.CENTRAL_WAREHOUSE,
                        "Integration Street 1"));
        locationIds.add(response.id());
        return response.id();
    }

    private void createInventory(UUID locationId, UUID productId, int quantity, int minThreshold)
    {
        InventoryResponse response = inventoryService.createInventory(
                new CreateInventoryRequest(locationId, productId, quantity, minThreshold));
        inventoryIds.add(response.id());
    }

    private UUID createTransferOrder(String orderNumber, UUID sourceId, UUID targetId)
    {
        TransferOrderResponse response = transferOrderService.createTransferOrder(
                new CreateTransferOrderRequest(orderNumber, sourceId, targetId));
        orderIds.add(response.id());
        return response.id();
    }
}