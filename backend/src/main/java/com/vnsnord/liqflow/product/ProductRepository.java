package com.vnsnord.liqflow.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Product} entities.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD and pagination
 * operations, plus the {@code sku}-based lookups declared below.</p>
 *
 * @see Product
 */
public interface ProductRepository extends JpaRepository<Product, UUID>
{
    /**
     * Finds a product by its unique SKU.
     *
     * @param sku the stock keeping unit to search for
     * @return the matching product, or an empty {@link Optional} if none exists
     */
    Optional<Product> findBySku(String sku);

    /**
     * Checks whether a product with the given SKU exists.
     *
     * @param sku the stock keeping unit to check
     * @return true if a product with the SKU exists, false otherwise
     */
    boolean existsBySku(String sku);
}
