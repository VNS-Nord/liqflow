package com.vnsnord.liqflow.common.exception;

/**
 * Thrown when a request conflicts with the current state of the resource, such
 * as when a record with the same unique key already exists, when a concurrent
 * update is detected, or when a data integrity constraint is violated.
 *
 * <p>All instances are translated to an HTTP 409 response by
 * {@link GlobalExceptionHandler}.</p>
 */
public class ConflictException extends RuntimeException
{
    /**
     * Creates a new conflict exception with the given detail message.
     *
     * @param message the detail message
     */
    public ConflictException(String message)
    {
        super(message);
    }
}