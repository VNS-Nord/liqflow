package com.vnsnord.liqflow.product;

import org.springframework.stereotype.Component;

/**
 * Maps between {@link Product} entities and their DTOs.
 */
@Component
public class ProductMapper
{
    /**
     * Converts a product entity into a response DTO.
     *
     * @param product the product entity
     * @return the response DTO
     */
    public ProductResponse toResponse(Product product)
    {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice()
        );
    }

    /**
     * Converts a create request into a new product entity.
     *
     * @param request the creation request
     * @return a new product entity
     */
    public Product toRequest(CreateProductRequest request)
    {
        return new Product(request.sku(),
                request.name(),
                request.description(),
                request.price());
    }
}