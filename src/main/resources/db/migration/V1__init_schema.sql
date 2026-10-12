-- =============================================================================
-- 1. LOCATIONS (Lokacijos: sandėliai, parduotuvės, habai)
-- =============================================================================
CREATE TABLE locations (
                           id UUID PRIMARY KEY,
                           code VARCHAR(50) NOT NULL,
                           name VARCHAR(100) NOT NULL,
                           type VARCHAR(30) NOT NULL,
                           address VARCHAR(255),
                           CONSTRAINT uk_location_code UNIQUE (code)
);

-- =============================================================================
-- 2. PRODUCTS (Prekės)
-- =============================================================================
CREATE TABLE products (
                          id UUID PRIMARY KEY,
                          sku VARCHAR(50) NOT NULL,
                          name VARCHAR(150) NOT NULL,
                          description VARCHAR(500),
                          price NUMERIC(12, 2) NOT NULL,
                          CONSTRAINT uk_product_sku UNIQUE (sku)
);

-- =============================================================================
-- 3. INVENTORIES (Likučiai lokacijose)
-- =============================================================================
CREATE TABLE inventories (
                             id UUID PRIMARY KEY,
                             location_id UUID NOT NULL,
                             product_id UUID NOT NULL,
                             quantity INT NOT NULL DEFAULT 0,
                             reserved_quantity INT NOT NULL DEFAULT 0,
                             min_threshold INT NOT NULL DEFAULT 0,
                             version BIGINT NOT NULL DEFAULT 0,
                             CONSTRAINT uk_location_product UNIQUE (location_id, product_id),
                             CONSTRAINT fk_inventories_location FOREIGN KEY (location_id) REFERENCES locations (id),
                             CONSTRAINT fk_inventories_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- Indeksai greitesnei paieškai pagal FK
CREATE INDEX idx_inventories_location ON inventories (location_id);
CREATE INDEX idx_inventories_product ON inventories (product_id);

-- =============================================================================
-- 4. TRANSFER_ORDERS (Perkėlimo užsakymai)
-- =============================================================================
CREATE TABLE transfer_orders (
                                 id UUID PRIMARY KEY,
                                 order_number VARCHAR(60) NOT NULL,
                                 source_location_id UUID NOT NULL,
                                 target_location_id UUID NOT NULL,
                                 status VARCHAR(30) NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL,
                                 updated_at TIMESTAMPTZ NOT NULL,
                                 version BIGINT NOT NULL DEFAULT 0,
                                 CONSTRAINT uk_transfer_order_number UNIQUE (order_number),
                                 CONSTRAINT fk_transfer_orders_source FOREIGN KEY (source_location_id) REFERENCES locations (id),
                                 CONSTRAINT fk_transfer_orders_target FOREIGN KEY (target_location_id) REFERENCES locations (id)
);

-- Indeksai paieškai pagal lokacijas ir būseną
CREATE INDEX idx_transfer_orders_source ON transfer_orders (source_location_id);
CREATE INDEX idx_transfer_orders_target ON transfer_orders (target_location_id);
CREATE INDEX idx_transfer_orders_status ON transfer_orders (status);

-- =============================================================================
-- 5. TRANSFER_ORDER_ITEMS (Perkėlimo užsakymo eilutės)
-- =============================================================================
CREATE TABLE transfer_order_items (
                                      id UUID PRIMARY KEY,
                                      transfer_order_id UUID NOT NULL,
                                      product_id UUID NOT NULL,
                                      quantity INT NOT NULL,
                                      CONSTRAINT uk_order_product UNIQUE (transfer_order_id, product_id),
                                      CONSTRAINT fk_items_transfer_order FOREIGN KEY (transfer_order_id) REFERENCES transfer_orders (id) ON DELETE CASCADE,
                                      CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- Indeksas užsakymo eilučių užklausoms
CREATE INDEX idx_items_transfer_order ON transfer_order_items (transfer_order_id);