package com.vnsnord.liqflow.common.exception;

/**
 * Base class for exceptions raised when a requested resource does not exist.
 *
 * <p>All subclasses are translated to an HTTP 404 response by
 * {@link GlobalExceptionHandler}.</p>
 */
public abstract class ResourceNotFoundException extends RuntimeException
{
    /**
     * Creates a new not-found exception with the given detail message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message)
    {
        super(message);
    }
}