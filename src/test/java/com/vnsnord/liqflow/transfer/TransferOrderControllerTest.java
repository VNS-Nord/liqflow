package com.vnsnord.liqflow.transfer;

import com.vnsnord.liqflow.common.exception.TransferOrderNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferOrderController.class)
public class TransferOrderControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferOrderService transferOrderService;

    @Test
    void getById_ShouldReturn200AndTransferOrderDto_WhenExists() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        UUID sourceLocationId = UUID.randomUUID();
        UUID targetLocationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-16T10:00:00Z");

        TransferOrderDetailResponse dto = new TransferOrderDetailResponse(
                id,
                "TR-10001",
                sourceLocationId,
                "WH-MAIN",
                targetLocationId,
                "WH-HUB",
                TransferOrderStatus.DRAFT,
                createdAt,
                List.of(new TransferOrderItemResponse(
                        UUID.randomUUID(), productId, "SKU-001", "Laptop", 2))
        );

        Mockito.when(transferOrderService.getTransferOrderDetailById(id)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/v1/transfer-orders/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.orderNumber").value("TR-10001"))
                .andExpect(jsonPath("$.sourceLocationId").value(sourceLocationId.toString()))
                .andExpect(jsonPath("$.sourceLocationCode").value("WH-MAIN"))
                .andExpect(jsonPath("$.targetLocationId").value(targetLocationId.toString()))
                .andExpect(jsonPath("$.targetLocationCode").value("WH-HUB"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.items[0].productSku").value("SKU-001"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void getById_ShouldReturn404_WhenNotFound() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        Mockito.when(transferOrderService.getTransferOrderDetailById(id))
                .thenThrow(new TransferOrderNotFoundException("id", id));

        // When & Then
        mockMvc.perform(get("/api/v1/transfer-orders/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/v1/transfer-orders/" + id));
    }

    @Test
    void getAll_ShouldReturn200AndPagedTransferOrders() throws Exception
    {
        // Given
        TransferOrderResponse dto = new TransferOrderResponse(
                UUID.randomUUID(),
                "TR-10001",
                UUID.randomUUID(),
                "WH-MAIN",
                UUID.randomUUID(),
                "WH-HUB",
                TransferOrderStatus.DRAFT,
                Instant.now()
        );

        Mockito.when(transferOrderService.getAllTransferOrders(any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        // When & Then
        mockMvc.perform(get("/api/v1/transfer-orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value("TR-10001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createTransferOrder_ShouldReturn201CreatedAndLocationHeader() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        TransferOrderResponse response = new TransferOrderResponse(
                id, "TR-10001", sourceId, "WH-MAIN", targetId, "WH-HUB",
                TransferOrderStatus.DRAFT, Instant.now());

        Mockito.when(transferOrderService.createTransferOrder(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/transfer-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "orderNumber": "TR-10001",
                                    "sourceLocationId": "%s",
                                    "targetLocationId": "%s"
                                }
                                """.formatted(sourceId, targetId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.orderNumber").value("TR-10001"));
    }

    @Test
    void addItem_ShouldReturn200AndUpdatedDetail() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        TransferOrderDetailResponse detail = new TransferOrderDetailResponse(
                id, "TR-10001", UUID.randomUUID(), "WH-MAIN", UUID.randomUUID(), "WH-HUB",
                TransferOrderStatus.DRAFT, Instant.now(),
                List.of(new TransferOrderItemResponse(UUID.randomUUID(), productId, "SKU-001", "Laptop", 3)));

        Mockito.when(transferOrderService.addItem(eq(id), any(TransferOrderItemRequest.class))).thenReturn(detail);

        // When & Then
        mockMvc.perform(post("/api/v1/transfer-orders/{id}/items", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productId": "%s",
                                    "quantity": 3
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }

    @Test
    void submit_ShouldReturn200AndSubmittedStatus() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        TransferOrderResponse response = new TransferOrderResponse(
                id, "TR-10001", UUID.randomUUID(), "WH-MAIN", UUID.randomUUID(), "WH-HUB",
                TransferOrderStatus.SUBMITTED, Instant.now());

        Mockito.when(transferOrderService.submit(id)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/transfer-orders/{id}/submit", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void complete_ShouldReturn200AndCompletedStatus() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        TransferOrderResponse response = new TransferOrderResponse(
                id, "TR-10001", UUID.randomUUID(), "WH-MAIN", UUID.randomUUID(), "WH-HUB",
                TransferOrderStatus.COMPLETED, Instant.now());

        Mockito.when(transferOrderService.complete(id)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/transfer-orders/{id}/complete", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void cancel_ShouldReturn200AndCancelledStatus() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        TransferOrderResponse response = new TransferOrderResponse(
                id, "TR-10001", UUID.randomUUID(), "WH-MAIN", UUID.randomUUID(), "WH-HUB",
                TransferOrderStatus.CANCELLED, Instant.now());

        Mockito.when(transferOrderService.cancel(id)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/transfer-orders/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
