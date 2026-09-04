package com.vnsnord.liqflow.domain.entity;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

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

    protected TransferOrderItem() {
    }

    public TransferOrderItem(TransferOrder transferOrder, Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.transferOrder = Objects.requireNonNull(transferOrder, "Transfer order cannot be null");
        this.product = Objects.requireNonNull(product, "Product cannot be null");
        this.quantity = quantity;
    }

    void addQuantity(int amount) {
        if (amount <= 0)
        {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        this.quantity += amount;
    }

    public UUID getId() {
        return id;
    }

    public TransferOrder getTransferOrder() {
        return transferOrder;
    }

    public Product getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
