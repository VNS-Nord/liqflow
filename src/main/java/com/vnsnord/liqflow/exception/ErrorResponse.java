package com.vnsnord.liqflow.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error response body returned by {@link GlobalExceptionHandler}.
 *
 * @param status           the HTTP status code
 * @param error            the HTTP status reason phrase
 * @param message          a human-readable description of the error
 * @param path             the request URI that produced the error
 * @param timestamp        the time at which the error occurred
 * @param validationErrors field-level validation messages, or null when not applicable
 */
public record ErrorResponse(int status,
                            String error,
                            String message,
                            String path,
                            Instant timestamp,
                            Map<String, String> validationErrors)
{
    /**
     * Creates an error response without field-level validation errors.
     *
     * @param status  the HTTP status code
     * @param error   the HTTP status reason phrase
     * @param message a human-readable description of the error
     * @param path    the request URI that produced the error
     */
    public ErrorResponse(int status, String error, String message, String path)
    {
        this(status, error, message, path, Instant.now(), null);
    }

    /**
     * Creates an error response with field-level validation errors.
     *
     * @param status           the HTTP status code
     * @param error            the HTTP status reason phrase
     * @param message          a human-readable description of the error
     * @param path             the request URI that produced the error
     * @param validationErrors the validation messages keyed by field name
     */
    public ErrorResponse(int status, String error, String message, String path, Map<String, String> validationErrors)
    {
        this(status, error, message, path, Instant.now(), validationErrors);
    }
}