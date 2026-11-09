package com.vnsnord.liqflow.product;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST endpoints for managing products under {@code /api/v1/products}.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController
{
    private final ProductService productService;

    /**
     * Creates a new product controller backed by the given service.
     *
     * @param productService the product application service
     */
    public ProductController(ProductService productService)
    {
        this.productService = productService;
    }

    /**
     * Returns a single product by its identifier.
     *
     * @param id the product identifier
     * @return the product with HTTP 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id)
    {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Returns a single product by its SKU.
     *
     * @param sku the stock keeping unit
     * @return the product with HTTP 200
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getBySku(@PathVariable String sku)
    {
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    /**
     * Returns a page of all products.
     *
     * @param pageable pagination and sorting information
     * @return the page of products with HTTP 200
     */
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAll(Pageable pageable)
    {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    /**
     * Creates a new product.
     *
     * @param request the creation request
     * @return the created product with a {@code Location} header and HTTP 201
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request)
    {
        ProductResponse response = productService.createProduct(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Updates the mutable details of an existing product.
     *
     * @param id      the product identifier
     * @param request the update request
     * @return the updated product with HTTP 200
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id,
                                                         @Valid @RequestBody UpdateProductRequest request)
    {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    /**
     * Deletes a product.
     *
     * @param id the product identifier
     * @return HTTP 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id)
    {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}