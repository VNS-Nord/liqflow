package com.vnsnord.liqflow.transfer;

import com.vnsnord.liqflow.product.Product;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

/**
 * A single line item within a {@link TransferOrder}, associating a {@link Product}
 * with a quantity to be transferred.
 *
 * <p>Each transfer order may contain at most one item per product. If
 * {@link TransferOrder#addItem(Product, int)} is called for a product that
 * already exists on the order, the quantity is increased rather than creating
 * a duplicate item.</p>
 *
 * @see TransferOrder
 * @see Product
 */
@Entity
@Table(
        name = "transfer_order_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_product",
                columnNames = {"transfer_order_id", "product_id"}
        )
)
public class TransferOrderItem
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_order_id", nullable = false)
    private TransferOrder transferOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    protected TransferOrderItem()
    {
    }

    /**
     * Creates a new transfer order item.
     *
     * @param transferOrder the parent transfer order (must not be null)
     * @param product       the product to transfer (must not be null)
     * @param quantity      the number of units to transfer (must be &gt; 0)
     * @throws IllegalArgumentException if {@code quantity} is &le; 0
     * @throws NullPointerException     if {@code transferOrder} or {@code product} is null
     */
    public TransferOrderItem(TransferOrder transferOrder, Product product, int quantity)
    {
        if (quantity <= 0)
        {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.transferOrder = Objects.requireNonNull(transferOrder, "Transfer order cannot be null");
        this.product = Objects.requireNonNull(product, "Product cannot be null");
        this.quantity = quantity;
    }

    /**
     * Increases the quantity for this item.
     *
     * @param amount the amount to add (must be &gt; 0)
     * @throws IllegalArgumentException if {@code amount} is &le; 0, or if the
     *                                  resulting quantity would not fit in an {@code Integer}
     */
    void addQuantity(int amount)
    {
        if (amount <= 0)
        {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        try
        {
            this.quantity = Math.addExact(this.quantity, amount);
        } catch (ArithmeticException exception)
        {
            throw new IllegalArgumentException("Quantity would exceed the maximum supported value", exception);
        }
    }

    /**
     * @return the item identifier
     */
    public UUID getId()
    {
        return id;
    }

    /**
     * @return the parent transfer order
     */
    public TransferOrder getTransferOrder()
    {
        return transferOrder;
    }

    /**
     * @return the product being transferred
     */
    public Product getProduct()
    {
        return product;
    }

    /**
     * @return the number of units to transfer
     */
    public Integer getQuantity()
    {
        return quantity;
    }
}
