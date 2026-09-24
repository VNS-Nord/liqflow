package com.vnsnord.liqflow.exception;

/**
 * Thrown when a transfer order cannot be found by the given identifier.
 */
public class TransferOrderNotFoundException extends ResourceNotFoundException
{
    /**
     * Creates a not-found exception describing the field and value that failed
     * to match a transfer order.
     *
     * @param fieldName  the name of the queried field
     * @param fieldValue the value of the queried field
     */
    public TransferOrderNotFoundException(String fieldName, Object fieldValue)
    {
        super(String.format("Transfer order not found with %s: '%s'", fieldName, fieldValue));
    }

    /**
     * Creates a not-found exception with the given detail message.
     *
     * @param message the detail message
     */
    public TransferOrderNotFoundException(String message)
    {
        super(message);
    }
}