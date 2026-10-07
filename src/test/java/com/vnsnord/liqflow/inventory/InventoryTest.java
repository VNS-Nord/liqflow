package com.vnsnord.liqflow.inventory;

import com.vnsnord.liqflow.location.Location;
import com.vnsnord.liqflow.location.LocationType;
import com.vnsnord.liqflow.product.Product;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

/**
 * Tests for the {@link Inventory} domain invariants, in particular the
 * reservation bookkeeping that keeps one order's stock out of another's reach.
 */
public class InventoryTest
{
    @Test
    void addStock_ShouldIncreaseQuantity()
    {
        Inventory inventory = new Inventory(location(), product(), 10, 0);

        inventory.addStock(5);

        Assertions.assertEquals(15, inventory.getQuantity());
    }

    @Test
    void addStock_ShouldRejectQuantitiesThatWouldOverflow()
    {
        Inventory inventory = new Inventory(location(), product(), Integer.MAX_VALUE, 0);

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> inventory.addStock(1));

        Assertions.assertEquals("Quantity would exceed the maximum supported value", exception.getMessage());
        Assertions.assertEquals(Integer.MAX_VALUE, inventory.getQuantity());
    }

    @Test
    void consumeReservedStock_ShouldRemoveFromBothQuantityAndReservedQuantity()
    {
        Inventory inventory = new Inventory(location(), product(), 10, 0);
        inventory.reserveStock(4);

        inventory.consumeReservedStock(4);

        Assertions.assertEquals(6, inventory.getQuantity());
        Assertions.assertEquals(0, inventory.getReservedQuantity());
        Assertions.assertEquals(6, inventory.getAvailableQuantity());
    }

    @Test
    void consumeReservedStock_ShouldNeverReachIntoUnreservedStock()
    {
        Inventory inventory = new Inventory(location(), product(), 10, 0);
        inventory.reserveStock(2);

        // This is the case the transfer order fix exists for: there is plenty of
        // physical stock, but only 2 units belong to the caller. The old
        // deduction path would have happily eaten the other 3.
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> inventory.consumeReservedStock(5));

        Assertions.assertEquals("Cannot consume more than currently reserved stock", exception.getMessage());
        Assertions.assertEquals(10, inventory.getQuantity());
        Assertions.assertEquals(2, inventory.getReservedQuantity());
        Assertions.assertEquals(8, inventory.getAvailableQuantity());
    }

    @Test
    void consumeReservedStock_ShouldLeaveAnotherHoldersReservationIntact()
    {
        Inventory inventory = new Inventory(location(), product(), 10, 0);
        inventory.reserveStock(5);
        inventory.reserveStock(3);

        inventory.consumeReservedStock(5);

        Assertions.assertEquals(5, inventory.getQuantity());
        Assertions.assertEquals(3, inventory.getReservedQuantity());
        Assertions.assertEquals(2, inventory.getAvailableQuantity());
    }

    @Test
    void releaseReservedStock_ShouldReturnUnitsToTheAvailableQuantity()
    {
        Inventory inventory = new Inventory(location(), product(), 10, 0);
        inventory.reserveStock(4);

        inventory.releaseReservedStock(4);

        Assertions.assertEquals(10, inventory.getQuantity());
        Assertions.assertEquals(0, inventory.getReservedQuantity());
        Assertions.assertEquals(10, inventory.getAvailableQuantity());
    }

    @Test
    void deductStock_ShouldRejectAMountThatWouldEatIntoReservedStock()
    {
        Inventory inventory = new Inventory(location(), product(), 10, 0);
        inventory.reserveStock(6);

        // 10 physical units, but 6 belong to a transfer order, so only 4 are
        // available. Deducting 5 must fail rather than quietly shrink the
        // reservation and strand the order.
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> inventory.deductStock(5));

        Assertions.assertEquals(
                "Cannot deduct more than the available stock; reserved units are not available",
                exception.getMessage());
        Assertions.assertEquals(10, inventory.getQuantity());
        Assertions.assertEquals(6, inventory.getReservedQuantity());
    }

    @Test
    void deductStock_ShouldRemoveAvailableStockOnly()
    {
        Inventory inventory = new Inventory(location(), product(), 10, 0);
        inventory.reserveStock(6);

        inventory.deductStock(4);

        Assertions.assertEquals(6, inventory.getQuantity());
        Assertions.assertEquals(6, inventory.getReservedQuantity());
        Assertions.assertEquals(0, inventory.getAvailableQuantity());
    }

    @Test
    void updateMinThreshold_ShouldChangeTheThresholdWithoutTouchingQuantity()
    {
        Inventory inventory = new Inventory(location(), product(), 10, 2);
        inventory.reserveStock(4);

        inventory.updateMinThreshold(7);

        Assertions.assertEquals(7, inventory.getMinThreshold());
        Assertions.assertEquals(10, inventory.getQuantity());
        Assertions.assertEquals(4, inventory.getReservedQuantity());
    }

    @Test
    void updateMinThreshold_ShouldRejectNegativeThresholds()
    {
        Inventory inventory = new Inventory(location(), product(), 10, 2);

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> inventory.updateMinThreshold(-1));

        Assertions.assertEquals("Minimum threshold cannot be negative", exception.getMessage());
        Assertions.assertEquals(2, inventory.getMinThreshold());
    }

    @Test
    void updateMinThreshold_ShouldAllowAThresholdAboveTheAvailableQuantity()
    {
        // Retuning the threshold so that the record reports as low stock is a
        // legitimate configuration, not an error.
        Inventory inventory = new Inventory(location(), product(), 10, 0);

        inventory.updateMinThreshold(50);

        Assertions.assertTrue(inventory.isBelowThreshold());
    }

    private Location location()
    {
        Location location = new Location("WH-TEST", "Test", LocationType.CENTRAL_WAREHOUSE, "Address");
        ReflectionTestUtils.setField(location, "id", UUID.randomUUID());
        return location;
    }

    private Product product()
    {
        Product product = new Product("SKU-TEST", "Test", "Description", java.math.BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        return product;
    }
}
