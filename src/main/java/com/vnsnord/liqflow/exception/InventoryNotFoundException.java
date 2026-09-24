package com.vnsnord.liqflow.exception;

/**
 * Thrown when an inventory record cannot be found for the given identifier or
 * for the given product/location combination.
 */
public class InventoryNotFoundException extends ResourceNotFoundException
{
    /**
     * Creates a not-found exception describing the field and value that failed
     * to match an inventory record.
     *
     * @param fieldName  the name of the queried field
     * @param fieldValue the value of the queried field
     */
    public InventoryNotFoundException(String fieldName, Object fieldValue)
    {
        super(String.format("Inventory not found with %s: '%s'", fieldName, fieldValue));
    }

    /**
     * Creates a not-found exception with the given detail message.
     *
     * @param message the detail message
     */
    public InventoryNotFoundException(String message)
    {
        super(message);
    }
}