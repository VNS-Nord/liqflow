package com.vnsnord.liqflow.exception;

/**
 * Thrown when a product cannot be found by the given identifier or SKU.
 */
public class ProductNotFoundException extends ResourceNotFoundException
{
    /**
     * Creates a not-found exception describing the field and value that failed
     * to match a product.
     *
     * @param fieldName  the name of the queried field
     * @param fieldValue the value of the queried field
     */
    public ProductNotFoundException(String fieldName, Object fieldValue)
    {
        this(String.format("Product not found with %s: '%s'", fieldName, fieldValue));
    }

    /**
     * Creates a not-found exception with the given detail message.
     *
     * @param message the detail message
     */
    public ProductNotFoundException(String message)
    {
        super(message);
    }
}