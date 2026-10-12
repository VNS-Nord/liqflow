package com.vnsnord.liqflow.common.exception;

/**
 * Thrown when a location cannot be found by the given identifier or code.
 */
public class LocationNotFoundException extends ResourceNotFoundException
{
    /**
     * Creates a not-found exception describing the field and value that failed
     * to match a location.
     *
     * @param fieldName  the name of the queried field
     * @param fieldValue the value of the queried field
     */
    public LocationNotFoundException(String fieldName, Object fieldValue)
    {
        this(String.format("Location not found with %s: '%s'", fieldName, fieldValue));
    }

    /**
     * Creates a not-found exception with the given detail message.
     *
     * @param message the detail message
     */
    public LocationNotFoundException(String message)
    {
        super(message);
    }
}