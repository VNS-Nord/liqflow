package com.vnsnord.liqflow.product;

import com.vnsnord.liqflow.common.exception.ProductNotFoundException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void getById_ShouldReturn200AndProductResponse_WhenExists() throws Exception
    {
        UUID id = UUID.randomUUID();
        ProductResponse response = new ProductResponse(id, "SKU-001", "Laptop", "High performance laptop", new BigDecimal("1200.00"));

        Mockito.when(productService.getProductById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(1200.00));
    }

    @Test
    void getById_ShouldReturn404_WhenNotFound() throws Exception
    {
        UUID id = UUID.randomUUID();
        Mockito.when(productService.getProductById(id))
                .thenThrow(new ProductNotFoundException("id", id));

        mockMvc.perform(get("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getAll_ShouldReturn200AndPagedProducts() throws Exception
    {
        ProductResponse response = new ProductResponse(UUID.randomUUID(), "SKU-001", "Laptop", "Desc", new BigDecimal("100.00"));

        Mockito.when(productService.getAllProducts(any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createProduct_ShouldReturn201CreatedAndLocationHeader() throws Exception
    {
        UUID id = UUID.randomUUID();
        ProductResponse response = new ProductResponse(id, "SKU-100", "New Product", "Desc", new BigDecimal("150.00"));

        Mockito.when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "sku": "SKU-100",
                                    "name": "New Product",
                                    "description": "Desc",
                                    "price": 150.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.endsWith("/api/v1/products/" + id)))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.sku").value("SKU-100"))
                .andExpect(jsonPath("$.name").value("New Product"));
    }

    @Test
    void createProduct_ShouldReturn400BadRequest_WhenValidationFails() throws Exception
    {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "sku": "",
                                    "name": "New Product",
                                    "price": -10.00
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProduct_ShouldReturn200AndUpdatedDto() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();
        ProductResponse response = new ProductResponse(id, "SKU-001", "Laptop Pro", "New desc", new BigDecimal("150.00"));

        Mockito.when(productService.updateProduct(eq(id), any(UpdateProductRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Laptop Pro",
                                    "description": "New desc",
                                    "price": 150.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.price").value(150.00));
    }

    @Test
    void updateProduct_ShouldReturn400_WhenValidationFails() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();

        // When & Then
        mockMvc.perform(put("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "",
                                    "price": -1.00
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProduct_ShouldReturn204NoContent() throws Exception
    {
        // Given
        UUID id = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
