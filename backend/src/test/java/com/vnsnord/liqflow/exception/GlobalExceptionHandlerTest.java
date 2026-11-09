package com.vnsnord.liqflow.exception;

import com.vnsnord.liqflow.common.exception.ConflictException;
import com.vnsnord.liqflow.common.exception.ErrorResponse;
import com.vnsnord.liqflow.common.exception.GlobalExceptionHandler;
import com.vnsnord.liqflow.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest
{
    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;
    @Mock
    private HttpServletRequest request;
    @Mock
    private MethodArgumentNotValidException validationException;
    @Mock
    private BindingResult bindingResult;

    @Test
    void handleResourceNotFound_ShouldReturn404AndCorrectErrorResponse()
    {
        // Given
        String path = "/api/v1/inventory/123";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        ResourceNotFoundException exception = new ResourceNotFoundException("Inventory not found with id: '123'")
        {
        };

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFound(exception, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(404, body.status());
        Assertions.assertEquals("Not Found", body.error());
        Assertions.assertEquals("Inventory not found with id: '123'", body.message());
        Assertions.assertEquals(path, body.path());
        Assertions.assertNotNull(body.timestamp());
        Assertions.assertNull(body.validationErrors());

        Mockito.verify(request).getRequestURI();
    }

    @Test
    void handleValidation_ShouldReturn400AndValidationErrorsMap()
    {
        // Given
        String path = "/api/v1/inventory";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        FieldError fieldError = new FieldError("inventoryDto", "quantity", "Quantity must be greater than zero");
        Mockito.when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        Mockito.when(bindingResult.getGlobalErrors()).thenReturn(List.of());
        Mockito.when(validationException.getBindingResult()).thenReturn(bindingResult);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidation(validationException, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(400, body.status());
        Assertions.assertEquals("Bad Request", body.error());
        Assertions.assertEquals("Validation failed", body.message());
        Assertions.assertEquals(path, body.path());
        Assertions.assertNotNull(body.validationErrors());
        Assertions.assertEquals("Quantity must be greater than zero", body.validationErrors().get("quantity"));

        Mockito.verify(request).getRequestURI();
    }

    @Test
    void handleIllegalArgument_ShouldReturn400AndErrorMessage()
    {
        // Given
        String path = "/api/v1/transfer-orders";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        IllegalArgumentException exception = new IllegalArgumentException("Invalid status transition");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgument(exception, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(400, body.status());
        Assertions.assertEquals("Bad Request", body.error());
        Assertions.assertEquals("Invalid status transition", body.message());
        Assertions.assertEquals(path, body.path());

        Mockito.verify(request).getRequestURI();
    }

    @Test
    void handleConflict_ShouldReturn409AndErrorMessage()
    {
        // Given
        String path = "/api/v1/products";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        ConflictException exception = new ConflictException("Product with SKU 'ABC' already exists");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleConflict(exception, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(409, body.status());
        Assertions.assertEquals("Conflict", body.error());
        Assertions.assertEquals("Product with SKU 'ABC' already exists", body.message());
        Assertions.assertEquals(path, body.path());

        Mockito.verify(request).getRequestURI();
    }

    @Test
    void handleTypeMismatch_ShouldReturn400AndParameterName()
    {
        // Given
        String path = "/api/v1/products/not-a-uuid";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException("not-a-uuid", UUID.class, "id", null, null);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTypeMismatch(exception, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(400, body.status());
        Assertions.assertEquals("Bad Request", body.error());
        Assertions.assertEquals("Invalid value 'not-a-uuid' for parameter 'id'", body.message());
        Assertions.assertEquals(path, body.path());

        Mockito.verify(request).getRequestURI();
    }

    @Test
    void handleMissingParameter_ShouldReturn400AndParameterName()
    {
        // Given
        String path = "/api/v1/locations";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("page", "int");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMissingParameter(exception, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(400, body.status());
        Assertions.assertEquals("Required parameter 'page' is missing", body.message());
        Assertions.assertEquals(path, body.path());

        Mockito.verify(request).getRequestURI();
    }

    @Test
    void handleDataIntegrityViolation_ShouldReturn409()
    {
        // Given
        String path = "/api/v1/inventory";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate key");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(409, body.status());

        Mockito.verify(request).getRequestURI();
    }

    @Test
    void handleOptimisticLocking_ShouldReturn409AndRetryMessage()
    {
        // Given
        String path = "/api/v1/transfer-orders/complete";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException("Inventory", UUID.randomUUID());

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleOptimisticLocking(exception, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals("Concurrent modification conflict; please retry", body.message());

        Mockito.verify(request).getRequestURI();
    }

    @Test
    void handlePessimisticLocking_ShouldReturn409AndRetryMessage()
    {
        // Given
        String path = "/api/v1/transfer-orders/complete";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        PessimisticLockingFailureException exception =
                new PessimisticLockingFailureException("could not lock row", new RuntimeException());

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handlePessimisticLocking(exception, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals("Could not acquire the required stock locks; please retry", body.message());

        Mockito.verify(request).getRequestURI();
    }

    @Test
    void handlePessimisticLocking_ShouldCoverDeadlocksAndLockTimeouts()
    {
        // Given: a deadlock and a lock-acquisition timeout are both reported
        // through the shared supertype, so one handler must cover both.
        Mockito.when(request.getRequestURI()).thenReturn("/api/v1/transfer-orders/complete");

        // When & Then
        Assertions.assertEquals(HttpStatus.CONFLICT,
                globalExceptionHandler.handlePessimisticLocking(
                                new PessimisticLockingFailureException("deadlock", new RuntimeException()), request)
                        .getStatusCode());
        Assertions.assertEquals(HttpStatus.CONFLICT,
                globalExceptionHandler.handlePessimisticLocking(
                                new CannotAcquireLockException("timeout"), request)
                        .getStatusCode());
    }

    @Test
    void handleConstraintViolation_ShouldReturn400AndViolationMessage()
    {
        // Given
        String path = "/api/v1/inventory";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        ConstraintViolationException exception =
                new ConstraintViolationException("quantity must be > 0", Set.of());

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleConstraintViolation(exception, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(400, body.status());
        Assertions.assertEquals("Validation failed", body.message());
        Assertions.assertEquals("quantity must be > 0", body.validationErrors().get("constraint"));

        Mockito.verify(request).getRequestURI();
    }

    @Test
    void handleGeneralException_ShouldReturn500AndGenericMessage()
    {
        // Given
        String path = "/api/v1/inventory";
        Mockito.when(request.getRequestURI()).thenReturn(path);

        RuntimeException exception = new RuntimeException("Database connection timeout");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGeneralException(exception, request);

        // Then
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        ErrorResponse body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(500, body.status());
        Assertions.assertEquals("Internal Server Error", body.error());
        Assertions.assertEquals("An unexpected internal server error occurred", body.message());
        Assertions.assertEquals(path, body.path());

        Mockito.verify(request).getRequestURI();
    }
}
