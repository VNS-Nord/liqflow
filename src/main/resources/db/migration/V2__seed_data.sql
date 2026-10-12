-- =============================================================================
-- 1. LOCATIONS (4 lokacijos: centrinis, regioninis habas ir 2 parduotuvės)
-- =============================================================================
INSERT INTO locations (id, code, name, type, address) VALUES
                                                          ('11111111-1111-1111-1111-111111111111', 'WH-CENTRAL', 'Centrinis Sandėlis', 'CENTRAL_WAREHOUSE', 'Pramonės g. 10, Kaunas'),
                                                          ('22222222-2222-2222-2222-222222222222', 'HUB-KAUNAS', 'Kauno Regioninis Habas', 'REGIONAL_HUB', 'Savanorių pr. 120, Kaunas'),
                                                          ('33333333-3333-3333-3333-333333333333', 'STORE-VILNIUS', 'Vilniaus Akropolio Parduotuvė', 'STORE', 'Ozo g. 25, Vilnius'),
                                                          ('44444444-4444-4444-4444-444444444444', 'STORE-KLAIPEDA', 'Klaipėdos Akropolio Parduotuvė', 'STORE', 'Taikos pr. 61, Klaipėda');

-- =============================================================================
-- 2. PRODUCTS (6 skirtingos prekės)
-- =============================================================================
INSERT INTO products (id, sku, name, description, price) VALUES
                                                             ('a1111111-1111-1111-1111-111111111111', 'PROD-LAPTOP-01', 'Lenovo ThinkPad X1', '14 colių verslo klasės nešiojamas kompiuteris', 1499.99),
                                                             ('a2222222-2222-2222-2222-222222222222', 'PROD-MOUSE-01', 'Logitech MX Master 3S', 'Ergonomiška belaidė kompiuterinė pelė', 109.90),
                                                             ('a3333333-3333-3333-3333-333333333333', 'PROD-KEYB-01', 'Keychron K2 V2', 'Belaidė mechaninė klaviatūra', 95.00),
                                                             ('a4444444-4444-4444-4444-444444444444', 'PROD-MON-01', 'Dell UltraSharp U2723QE', '27 colių 4K IPS monitorius', 589.00),
                                                             ('a5555555-5555-5555-5555-555555555555', 'PROD-HUB-01', 'Anker 7-in-1 USB-C Hub', 'USB-C adapteris su HDMI ir SD skaitytuvu', 45.50),
                                                             ('a6666666-6666-6666-6666-666666666666', 'PROD-HEAD-01', 'Sony WH-1000XM5', 'Triukšmą slopinančios belaidės ausinės', 349.00);

-- =============================================================================
-- 3. INVENTORIES (Likučiai centriniame sandėlyje ir regioniniame habe)
-- =============================================================================

-- Centrinis Sandėlis (WH-CENTRAL)
INSERT INTO inventories (id, location_id, product_id, quantity, reserved_quantity, min_threshold, version) VALUES
                                                                                                               ('b1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 50, 0, 5, 0),
                                                                                                               ('b2222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', 200, 0, 20, 0),
                                                                                                               ('b3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'a3333333-3333-3333-3333-333333333333', 150, 0, 15, 0),
                                                                                                               ('b4444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', 'a4444444-4444-4444-4444-444444444444', 30, 0, 3, 0),
                                                                                                               ('b5555555-5555-5555-5555-555555555555', '11111111-1111-1111-1111-111111111111', 'a5555555-5555-5555-5555-555555555555', 300, 0, 30, 0),
                                                                                                               ('b6666666-6666-6666-6666-666666666666', '11111111-1111-1111-1111-111111111111', 'a6666666-6666-6666-6666-666666666666', 80, 0, 10, 0);

-- Kauno Regioninis Habas (HUB-KAUNAS)
INSERT INTO inventories (id, location_id, product_id, quantity, reserved_quantity, min_threshold, version) VALUES
                                                                                                               ('c1111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', 10, 0, 2, 0),
                                                                                                               ('c2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222', 40, 0, 5, 0),
                                                                                                               ('c3333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 'a5555555-5555-5555-5555-555555555555', 50, 0, 10, 0);