package com.vnsnord.liqflow.product;

import com.vnsnord.liqflow.common.exception.ConflictException;
import com.vnsnord.liqflow.common.exception.ProductNotFoundException;
import com.vnsnord.liqflow.inventory.InventoryRepository;
import com.vnsnord.liqflow.transfer.TransferOrderRepository;
import com.vnsnord.liqflow.transfer.TransferOrderReservationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service handling product read, creation, update, and deletion.
 */
@Service
@Transactional(readOnly = true)
public class ProductService
{
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final TransferOrderRepository transferOrderRepository;
    private final TransferOrderReservationRepository reservationRepository;
    private final ProductMapper productMapper;

    /**
     * Creates a new product service with the given dependencies.
     *
     * @param productRepository        the product repository
     * @param inventoryRepository      the inventory repository
     * @param transferOrderRepository  the transfer order repository
     * @param reservationRepository    the transfer order reservation repository
     * @param productMapper            the product mapper
     */
    public ProductService(ProductRepository productRepository,
                          InventoryRepository inventoryRepository,
                          TransferOrderRepository transferOrderRepository,
                          TransferOrderReservationRepository reservationRepository,
                          ProductMapper productMapper)
    {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.transferOrderRepository = transferOrderRepository;
        this.reservationRepository = reservationRepository;
        this.productMapper = productMapper;
    }

    /**
     * Finds a product by its identifier.
     *
     * @param id the product identifier
     * @return the corresponding response DTO
     * @throws ProductNotFoundException if no product exists with the given id
     */
    public ProductResponse getProductById(UUID id)
    {
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException("id", id));
    }

    /**
     * Finds a product by its SKU.
     *
     * @param sku the stock keeping unit
     * @return the corresponding response DTO
     * @throws ProductNotFoundException if no product exists with the given SKU
     */
    public ProductResponse getProductBySku(String sku)
    {
        return productRepository.findBySku(sku.trim())
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException("sku", sku.trim()));
    }

    /**
     * Returns a page of all products.
     *
     * @param pageable pagination and sorting information
     * @return a page of response DTOs
     */
    public Page<ProductResponse> getAllProducts(Pageable pageable)
    {
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    /**
     * Creates a new product.
     *
     * @param request the creation request
     * @return the response DTO of the saved product
     * @throws ConflictException if a product with the same SKU already exists
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request)
    {
        if (productRepository.existsBySku(request.sku().trim()))
        {
            throw new ConflictException("Product with SKU '" + request.sku().trim() + "' already exists");
        }
        Product product = productMapper.toRequest(request);
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Updates the mutable details of an existing product.
     *
     * @param id      the product identifier
     * @param request the update request
     * @return the response DTO of the updated product
     * @throws ProductNotFoundException if no product exists with the given id
     */
    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request)
    {
        Product product = getProduct(id);
        product.updateDetails(request.name(), request.description(), request.price());
        return productMapper.toResponse(productRepository.save(product));
    }

    /**
     * Deletes a product.
     *
     * @param id the product identifier
     * @throws ProductNotFoundException if no product exists with the given id
     * @throws IllegalStateException    if the product is referenced by inventory records,
     *                                  transfer order items, or transfer order reservations
     */
    @Transactional
    public void deleteProduct(UUID id)
    {
        Product product = getProduct(id);
        if (inventoryRepository.existsByProductId(id))
        {
            throw new IllegalStateException("Product cannot be deleted because it has inventory records");
        }
        if (transferOrderRepository.existsItemForProduct(id))
        {
            throw new IllegalStateException("Product cannot be deleted because it is referenced by transfer order items");
        }
        if (reservationRepository.existsByProductId(id))
        {
            throw new IllegalStateException("Product cannot be deleted because it is referenced by transfer order reservations");
        }
        productRepository.delete(product);
    }

    /**
     * Loads a product by identifier or throws when it does not exist.
     *
     * @param id the product identifier
     * @return the product entity
     * @throws ProductNotFoundException if no product exists with the given id
     */
    private Product getProduct(UUID id)
    {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("id", id));
    }
}