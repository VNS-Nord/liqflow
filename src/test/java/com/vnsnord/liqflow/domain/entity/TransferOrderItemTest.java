package com.vnsnord.liqflow.domain.entity;

import com.vnsnord.liqflow.domain.enums.LocationType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tests for the {@link TransferOrderItem} quantity invariants, focusing on the
 * merge path that {@link TransferOrder#addItem(Product, int)} takes when the
 * same product is added twice.
 */
public class TransferOrderItemTest
{
    @Test
    void addQuantity_ShouldMergeIntoTheExistingItem()
    {
        TransferOrder order = order();
        Product product = product();

        order.addItem(product, 3);
        order.addItem(product, 4);

        Assertions.assertEquals(1, order.getItems().size());
        Assertions.assertEquals(7, order.getItems().getFirst().getQuantity());
    }

    @Test
    void addQuantity_ShouldRejectMergedQuantitiesThatWouldOverflow()
    {
        TransferOrder order = order();
        Product product = product();

        order.addItem(product, Integer.MAX_VALUE);

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> order.addItem(product, 1));

        Assertions.assertEquals("Quantity would exceed the maximum supported value", exception.getMessage());
        Assertions.assertEquals(Integer.MAX_VALUE, order.getItems().getFirst().getQuantity());
    }

    private TransferOrder order()
    {
        return new TransferOrder("TR-TEST", location("WH-A"), location("WH-B"));
    }

    private Location location(String code)
    {
        Location location = new Location(code, code, LocationType.CENTRAL_WAREHOUSE, "Address");
        ReflectionTestUtils.setField(location, "id", UUID.randomUUID());
        return location;
    }

    private Product product()
    {
        Product product = new Product("SKU-TEST", "Test", "Description", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        return product;
    }
}
