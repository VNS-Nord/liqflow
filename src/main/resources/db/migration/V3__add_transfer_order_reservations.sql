CREATE TABLE transfer_order_reservations (
                                             id UUID PRIMARY KEY,
                                             transfer_order_id UUID NOT NULL,
                                             transfer_order_item_id UUID NOT NULL,
                                             product_id UUID NOT NULL,
                                             location_id UUID NOT NULL,
                                             quantity INT NOT NULL,
                                             status VARCHAR(30) NOT NULL,
                                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                             version BIGINT NOT NULL DEFAULT 0,
                                             CONSTRAINT uk_order_item UNIQUE (transfer_order_item_id),
                                             CONSTRAINT fk_reserved_order FOREIGN KEY (transfer_order_id) REFERENCES transfer_orders (id) ON DELETE CASCADE,
                                             CONSTRAINT fk_reserved_item FOREIGN KEY (transfer_order_item_id) REFERENCES transfer_order_items (id) ON DELETE CASCADE,
                                             CONSTRAINT fk_reserved_product FOREIGN KEY (product_id) REFERENCES products (id),
                                             CONSTRAINT fk_reserved_location FOREIGN KEY (location_id) REFERENCES locations (id)
);

CREATE INDEX idx_transfer_order ON transfer_order_reservations (transfer_order_id);
CREATE INDEX idx_reservation_product ON transfer_order_reservations (product_id);
CREATE INDEX idx_reservation_location_product_status ON transfer_order_reservations (location_id, product_id, status);