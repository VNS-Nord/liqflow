package com.vnsnord.liqflow.controller;

import com.vnsnord.liqflow.dto.request.CreateInventoryRequest;
import com.vnsnord.liqflow.dto.response.InventoryResponse;
import com.vnsnord.liqflow.exception.InventoryNotFoundException;
import com.vnsnord.liqflow.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
public class InventoryControllerTest
{
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void getById_ShouldReturn200AndInventoryDto_WhenExists() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryResponse dto = new InventoryResponse(
                id,
                locationId,
                "WH-MAIN",
                productId,
                "SKU-001",
                "Laptop",
                100,
                10,
                90,
                5
        );

        Mockito.when(inventoryService.getInventoryById(id)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/v1/inventories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.locationId").value(locationId.toString()))
                .andExpect(jsonPath("$.locationCode").value("WH-MAIN"))
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.productSku").value("SKU-001"))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.reservedQuantity").value(10))
                .andExpect(jsonPath("$.availableQuantity").value(90))
                .andExpect(jsonPath("$.minThreshold").value(5));
    }

    @Test
    void getById_ShouldReturn404_WhenNotFound() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        Mockito.when(inventoryService.getInventoryById(id))
                .thenThrow(new InventoryNotFoundException("id", id));

        // When & Then
        mockMvc.perform(get("/api/v1/inventories/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/v1/inventories/" + id));
    }

    @Test
    void getByLocation_ShouldReturn200AndPagedResult() throws Exception
    {
        // Given
        UUID locationId = UUID.randomUUID();
        InventoryResponse dto = new InventoryResponse(
                UUID.randomUUID(), locationId, "WH-MAIN", UUID.randomUUID(), "SKU-001", "Laptop", 50, 0, 50, 5
        );

        Mockito.when(inventoryService.getInventoryByLocation(eq(locationId), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        // When & Then
        mockMvc.perform(get("/api/v1/inventories/location/{locationId}", locationId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].locationId").value(locationId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getLowStock_ShouldReturn200AndList() throws Exception
    {
        // Given
        InventoryResponse dto = new InventoryResponse(
                UUID.randomUUID(), UUID.randomUUID(), "WH-MAIN", UUID.randomUUID(), "SKU-001", "Laptop", 2, 0, 2, 5
        );

        Mockito.when(inventoryService.getLowStockInventory()).thenReturn(List.of(dto));

        // When & Then
        mockMvc.perform(get("/api/v1/inventories/low-stock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].minThreshold").value(5));
    }

    @Test
    void createInventory_ShouldReturn201CreatedAndLocationHeader() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventoryResponse response = new InventoryResponse(
                id, locationId, "WH-MAIN", productId, "SKU-001", "Laptop", 10, 0, 10, 2);

        Mockito.when(inventoryService.createInventory(any(CreateInventoryRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "locationId": "%s",
                                    "productId": "%s",
                                    "initialStock": 10,
                                    "minThreshold": 2
                                }
                                """.formatted(locationId, productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void createInventory_ShouldReturn400_WhenValidationFails() throws Exception
    {
        // When & Then
        mockMvc.perform(post("/api/v1/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "initialStock": -5,
                                    "minThreshold": -1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addStock_ShouldReturn200AndUpdatedQuantity() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        InventoryResponse response = new InventoryResponse(
                id, UUID.randomUUID(), "WH-MAIN", UUID.randomUUID(), "SKU-001", "Laptop", 15, 0, 15, 2);

        Mockito.when(inventoryService.addStock(eq(id), eq(5))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/inventories/{id}/add-stock", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(15));
    }

    @Test
    void reserveEndpoint_ShouldNotExist() throws Exception
    {
        // Reservations are only created by submitting a transfer order, so that
        // reservedQuantity always has a matching reservation row. There is no
        // way to reserve stock without one.
        mockMvc.perform(post("/api/v1/inventories/{id}/reserve", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 4
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void releaseEndpoint_ShouldNotExist() throws Exception
    {
        // A held reservation can only be settled by completing or cancelling its
        // transfer order. Releasing reservedQuantity directly would let a
        // submitted order fail at completion.
        mockMvc.perform(post("/api/v1/inventories/{id}/release", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 4
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void addStock_ShouldReturn400_WhenValidationFails() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();

        // When & Then
        mockMvc.perform(post("/api/v1/inventories/{id}/add-stock", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
