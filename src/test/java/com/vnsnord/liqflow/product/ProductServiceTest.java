package com.vnsnord.liqflow.product;

import com.vnsnord.liqflow.common.exception.ConflictException;
import com.vnsnord.liqflow.common.exception.ProductNotFoundException;
import com.vnsnord.liqflow.inventory.InventoryRepository;
import com.vnsnord.liqflow.transfer.TransferOrderRepository;
import com.vnsnord.liqflow.transfer.TransferOrderReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest
{
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private TransferOrderRepository transferOrderRepository;
    @Mock
    private TransferOrderReservationRepository reservationRepository;
    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProductById_ShouldReturnProductResponseDto_WhenProductExists()
    {
        // Given
        UUID productId = UUID.randomUUID();
        Product product = new Product("SKU-001", "Laptop", "High-performance laptop", new BigDecimal("1200.00"));
        ReflectionTestUtils.setField(product, "id", productId);

        ProductResponse expectedDto = new ProductResponse(
                productId,
                "SKU-001",
                "Laptop",
                "High-performance laptop",
                new BigDecimal("1200.00")
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(expectedDto);

        // When
        ProductResponse actualDto = productService.getProductById(productId);

        // Then
        assertNotNull(actualDto);
        assertEquals(expectedDto.id(), actualDto.id());
        assertEquals(expectedDto.sku(), actualDto.sku());
        assertEquals(expectedDto.price(), actualDto.price());

        verify(productRepository).findById(productId);
        verify(productMapper).toResponse(product);
    }

    @Test
    void getProductById_ShouldThrowProductNotFoundException_WhenProductDoesNotExist()
    {
        // Given
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // When & Then
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(productId)
        );

        assertEquals("Product not found with id: '" + productId + "'", exception.getMessage());

        verify(productRepository).findById(productId);
        Mockito.verifyNoInteractions(productMapper);
    }

    @Test
    void getProductBySku_ShouldReturnProductResponseDto_WhenProductExists()
    {
        // Given
        String sku = "SKU-001";
        UUID productId = UUID.randomUUID();
        Product product = new Product(sku, "Laptop", "High-performance laptop", new BigDecimal("1200.00"));
        ReflectionTestUtils.setField(product, "id", productId);

        ProductResponse expectedDto = new ProductResponse(
                productId,
                sku,
                "Laptop",
                "High-performance laptop",
                new BigDecimal("1200.00")
        );

        when(productRepository.findBySku(sku)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(expectedDto);

        // When
        ProductResponse actualDto = productService.getProductBySku(sku);

        // Then
        assertNotNull(actualDto);
        assertEquals(expectedDto.sku(), actualDto.sku());

        verify(productRepository).findBySku(sku);
        verify(productMapper).toResponse(product);
    }

    @Test
    void getProductBySku_ShouldThrowProductNotFoundException_WhenProductDoesNotExist()
    {
        // Given
        String sku = "NON-EXISTENT";
        when(productRepository.findBySku(sku)).thenReturn(Optional.empty());

        // When & Then
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductBySku(sku)
        );

        assertEquals("Product not found with sku: '" + sku + "'", exception.getMessage());

        verify(productRepository).findBySku(sku);
        Mockito.verifyNoInteractions(productMapper);
    }

    @Test
    void getAllProducts_ShouldReturnListOfProductResponseDtos()
    {
        Pageable pageable = PageRequest.of(0, 10);
        UUID productId = UUID.randomUUID();
        Product product = createProduct(productId, "SKU-001", "Laptop", "Description", new BigDecimal("100.00"));

        ProductResponse dto = new ProductResponse(
                productId,
                "SKU-001",
                "Laptop",
                "Description",
                new BigDecimal("100.00")
        );

        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findAll(pageable)).thenReturn(productPage);
        when(productMapper.toResponse(product)).thenReturn(dto);

        // When
        Page<ProductResponse> result = productService.getAllProducts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(productId, result.getContent().getFirst().id());

        verify(productRepository).findAll(pageable);
        verify(productMapper).toResponse(product);
    }

    @Test
    void createProduct_ShouldSaveAndReturnProductResponse_WhenSkuIsUnique()
    {
        // Given
        CreateProductRequest request = new CreateProductRequest("SKU-100", "New Product", "Desc", new BigDecimal("150.00"));
        Product product = new Product("SKU-100", "New Product", "Desc", new BigDecimal("150.00"));
        UUID generatedId = UUID.randomUUID();
        Product savedProduct = createProduct(generatedId, "SKU-100", "New Product", "Desc", new BigDecimal("150.00"));
        ProductResponse expectedResponse = new ProductResponse(generatedId, "SKU-100", "New Product", "Desc", new BigDecimal("150.00"));

        when(productRepository.existsBySku("SKU-100")).thenReturn(false);
        when(productMapper.toRequest(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(savedProduct);
        when(productMapper.toResponse(savedProduct)).thenReturn(expectedResponse);

        // When
        ProductResponse result = productService.createProduct(request);

        // Then
        assertNotNull(result);
        assertEquals(generatedId, result.id());
        assertEquals("SKU-100", result.sku());
        verify(productRepository).save(product);
    }

    @Test
    void createProduct_ShouldThrowException_WhenSkuAlreadyExists()
    {
        // Given
        CreateProductRequest request = new CreateProductRequest("SKU-100", "New Product", "Desc", new BigDecimal("150.00"));
        when(productRepository.existsBySku("SKU-100")).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> productService.createProduct(request)
        );

        assertEquals("Product with SKU 'SKU-100' already exists", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_ShouldUpdateDetailsAndReturnResponse()
    {
        // Given
        UUID productId = UUID.randomUUID();
        Product product = new Product("SKU-001", "Laptop", "Old desc", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(product, "id", productId);
        Product savedProduct = new Product("SKU-001", "Laptop Pro", "New desc", new BigDecimal("150.00"));
        ReflectionTestUtils.setField(savedProduct, "id", productId);

        UpdateProductRequest request = new UpdateProductRequest("Laptop Pro", "New desc", new BigDecimal("150.00"));
        ProductResponse expectedResponse = new ProductResponse(productId, "SKU-001", "Laptop Pro", "New desc", new BigDecimal("150.00"));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(savedProduct);
        when(productMapper.toResponse(savedProduct)).thenReturn(expectedResponse);

        // When
        ProductResponse result = productService.updateProduct(productId, request);

        // Then
        assertNotNull(result);
        assertEquals("Laptop Pro", result.name());
        assertEquals(new BigDecimal("150.00"), result.price());
        assertEquals("Laptop Pro", product.getName());
        assertEquals("New desc", product.getDescription());
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_ShouldThrowProductNotFoundException_WhenProductDoesNotExist()
    {
        // Given
        UUID productId = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest("Laptop Pro", "Desc", new BigDecimal("150.00"));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(productId, request));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_ShouldDelete_WhenNotReferenced()
    {
        // Given
        UUID productId = UUID.randomUUID();
        Product product = new Product("SKU-001", "Laptop", "Desc", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(product, "id", productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(productId)).thenReturn(false);
        when(transferOrderRepository.existsItemForProduct(productId)).thenReturn(false);
        when(reservationRepository.existsByProductId(productId)).thenReturn(false);

        // When
        productService.deleteProduct(productId);

        // Then
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_ShouldThrow_WhenReferencedByReservation()
    {
        // Given
        UUID productId = UUID.randomUUID();
        Product product = new Product("SKU-001", "Laptop", "Desc", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(product, "id", productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(productId)).thenReturn(false);
        when(transferOrderRepository.existsItemForProduct(productId)).thenReturn(false);
        when(reservationRepository.existsByProductId(productId)).thenReturn(true);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> productService.deleteProduct(productId));
        assertEquals("Product cannot be deleted because it is referenced by transfer order reservations",
                exception.getMessage());
        verify(productRepository, never()).delete(any());
    }

    @Test
    void deleteProduct_ShouldThrow_WhenReferencedByInventory()
    {
        // Given
        UUID productId = UUID.randomUUID();
        Product product = new Product("SKU-001", "Laptop", "Desc", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(product, "id", productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(inventoryRepository.existsByProductId(productId)).thenReturn(true);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> productService.deleteProduct(productId)
        );
        assertEquals("Product cannot be deleted because it has inventory records", exception.getMessage());
        verify(productRepository, never()).delete(any());
    }

    private Product createProduct(UUID id, String sku, String name, String description, BigDecimal price)
    {
        Product product = new Product(sku, name, description, new BigDecimal("100.00"));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
