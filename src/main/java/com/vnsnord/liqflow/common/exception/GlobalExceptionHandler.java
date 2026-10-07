package com.vnsnord.liqflow.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central exception handler that converts exceptions thrown by controllers
 * into a uniform {@link ErrorResponse} body.
 *
 * <p>Resource-not-found errors map to 404, validation errors and illegal
 * domain operations map to 400, duplicate or concurrent-modification conflicts
 * map to 409, and every other exception maps to 500.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    /**
     * Handles exceptions raised when a requested resource does not exist.
     *
     * @param ex      the not-found exception
     * @param request the current HTTP request
     * @return a 404 response with the exception message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request)
    {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /**
     * Handles requests that conflict with the current state of a resource,
     * such as duplicate unique keys and concurrent modification failures.
     *
     * @param ex      the conflict exception
     * @param request the current HTTP request
     * @return a 409 response with the exception message
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request)
    {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /**
     * Handles bean-validation failures raised on request payloads.
     *
     * @param ex      the validation exception
     * @param request the current HTTP request
     * @return a 400 response with the field-level validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request)
    {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors())
        {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors())
        {
            errors.put(error.getObjectName(), error.getDefaultMessage());
        }

        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    /**
     * Handles constraint violations raised on method parameters or constructor
     * arguments.
     *
     * @param ex      the constraint violation exception
     * @param request the current HTTP request
     * @return a 400 response with the field-level constraint errors
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request)
    {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations())
        {
            String field = violation.getPropertyPath().toString();
            errors.put(field, violation.getMessage());
        }
        if (errors.isEmpty())
        {
            errors.put("constraint", ex.getMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    /**
     * Handles malformed or unreadable request bodies.
     *
     * @param ex      the unreadable-message exception
     * @param request the current HTTP request
     * @return a 400 response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex, HttpServletRequest request)
    {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request, null);
    }

    /**
     * Handles method-argument type mismatches, for example a non-UUID value
     * supplied for a path variable of type {@link java.util.UUID}.
     *
     * @param ex      the argument type mismatch exception
     * @param request the current HTTP request
     * @return a 400 response describing the offending parameter
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request)
    {
        String message = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    /**
     * Handles missing required query or form parameters.
     *
     * @param ex      the missing-parameter exception
     * @param request the current HTTP request
     * @return a 400 response describing the missing parameter
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request)
    {
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    /**
     * Handles data-integrity violations such as unique-constraint races that
     * slip through the pre-flight existence checks.
     *
     * @param ex      the integrity violation exception
     * @param request the current HTTP request
     * @return a 409 response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request)
    {
        return build(HttpStatus.CONFLICT, "Data conflict (duplicate or referential integrity violation)", request, null);
    }

    /**
     * Handles optimistic-locking failures raised when a record was modified by
     * another transaction since it was read.
     *
     * @param ex      the optimistic-locking exception
     * @param request the current HTTP request
     * @return a 409 response advising the client to retry
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex, HttpServletRequest request)
    {
        return build(HttpStatus.CONFLICT, "Concurrent modification conflict; please retry", request, null);
    }

    /**
     * Handles illegal arguments and illegal state transitions thrown by the
     * domain and service layers.
     *
     * @param ex      the runtime exception
     * @param request the current HTTP request
     * @return a 400 response with the exception message
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgument(RuntimeException ex, HttpServletRequest request)
    {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    /**
     * Handles pessimistic-lock failures, covering both lock-acquisition timeouts
     * and deadlocks. Both are transient and both share the optimistic-locking
     * remedy of asking the client to retry.
     *
     * @param ex      the pessimistic-locking exception
     * @param request the current HTTP request
     * @return a 409 response advising the client to retry
     */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handlePessimisticLocking(PessimisticLockingFailureException ex, HttpServletRequest request)
    {
        return build(HttpStatus.CONFLICT, "Could not acquire the required stock locks; please retry", request, null);
    }

    /**
     * Handles requests that match no controller mapping. Without this handler the
     * catch-all below would report an unknown URL as a 500, which blames the
     * server for what is really a client mistake.
     *
     * @param ex      the exception raised while resolving the handler
     * @param request the current HTTP request
     * @return a 404 response naming the path that was not found
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request)
    {
        return build(HttpStatus.NOT_FOUND, "No endpoint for " + request.getRequestURI(), request, null);
    }

    /**
     * Handles a request whose path exists but not for the method that was used.
     *
     * <p>Without this handler the {@link Exception} fallback below reports a
     * wrong method as a 500, which reads as a server fault when it is really a
     * client mistake.</p>
     *
     * @param ex      the unsupported method exception
     * @param request the current HTTP request
     * @return a 405 response naming the path and listing the methods it supports
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request)
    {
        String supported = ex.getSupportedHttpMethods() == null
                ? ""
                : " Allowed methods: " + ex.getSupportedHttpMethods().stream()
                .map(HttpMethod::name)
                .collect(Collectors.joining(", "));

        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "Method " + ex.getMethod() + " is not supported for " + request.getRequestURI() + "." + supported,
                request, null);
    }

    /**
     * Handles a request whose body declares a media type the endpoint cannot read.
     *
     * @param ex      the unsupported media type exception
     * @param request the current HTTP request
     * @return a 415 response naming the unsupported content type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpServletRequest request)
    {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type " + ex.getContentType() + " is not supported for " + request.getRequestURI(),
                request, null);
    }

    /**
     * Fallback handler for any exception not handled above.
     *
     * @param ex      the unexpected exception
     * @param request the current HTTP request
     * @return a 500 response with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request)
    {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred", request, null);
    }

    /**
     * Builds a uniform error-response body and the matching HTTP status.
     *
     * @param status           the HTTP status
     * @param message          the human-readable error message
     * @param request          the current HTTP request
     * @param validationErrors field-level validation messages, or null
     * @return the response entity
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request, Map<String, String> validationErrors)
    {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}