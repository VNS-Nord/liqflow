INSERT INTO roles (id, code, name, description)
VALUES (gen_random_uuid(), 'ADMIN', 'Administrator',
        'Manages user accounts, roles, permissions assignments, locations, and the product catalog.'),
       (gen_random_uuid(), 'WAREHOUSE_MANAGER', 'Warehouse Manager',
        'Manages inventory and transfer orders for assigned warehouses.'),
       (gen_random_uuid(), 'STORE_MANAGER', 'Store Manager',
        'Manages inventory and incoming and outgoing transfer orders for assigned stores.'),
       (gen_random_uuid(), 'VIEWER', 'Viewer',
        'Views products, inventory, and transfer orders for assigned locations without modifying data.'),
       (gen_random_uuid(), 'PICKER', 'Picker',
        'Views assigned picking tasks and records picked quantities at assigned source locations.');

INSERT INTO permissions (id, code, description)
VALUES (gen_random_uuid(), 'PRODUCT_READ', 'View the shared product catalog.'),
       (gen_random_uuid(), 'PRODUCT_WRITE', 'Create, update, and delete products.'),
       (gen_random_uuid(), 'LOCATION_READ', 'View accessible locations.'),
       (gen_random_uuid(), 'LOCATION_MANAGE', 'Create, update, and delete locations.'),
       (gen_random_uuid(), 'INVENTORY_READ', 'View inventory levels and low-stock records for assigned locations.'),
       (gen_random_uuid(), 'INVENTORY_CREATE', 'Create inventory records for assigned locations.'),
       (gen_random_uuid(), 'INVENTORY_CONFIGURE', 'Update minimum stock thresholds for assigned locations.'),
       (gen_random_uuid(), 'INVENTORY_ADJUST', 'Add or deduct stock at assigned locations.'),
       (gen_random_uuid(), 'TRANSFER_ORDER_READ', 'View transfer orders and their items for accessible locations.'),
       (gen_random_uuid(), 'TRANSFER_ORDER_CREATE', 'Create draft transfer orders from assigned source locations.'),
       (gen_random_uuid(), 'TRANSFER_ORDER_EDIT',
        'Add and remove items in draft transfer orders from assigned source locations.'),
       (gen_random_uuid(), 'TRANSFER_ORDER_SUBMIT',
        'Submit transfer orders and reserve stock at assigned source locations.'),
       (gen_random_uuid(), 'TRANSFER_ORDER_DISPATCH',
        'Mark transfer orders from assigned source locations as in transit.'),
       (gen_random_uuid(), 'TRANSFER_ORDER_RECEIVE',
        'Confirm receipt and complete transfer orders at assigned target locations.'),
       (gen_random_uuid(), 'TRANSFER_ORDER_CANCEL',
        'Cancel transfer orders from assigned source locations when their status allows it.'),
       (gen_random_uuid(), 'USER_MANAGE', 'Create user accounts and manage their active status.'),
       (gen_random_uuid(), 'ACCESS_MANAGE',
        'Manage roles, assign permissions to roles, and assign roles and locations to users.'),
       (gen_random_uuid(), 'PICKING_READ', 'View picking tasks and their items within the permitted scope.'),
       (gen_random_uuid(), 'PICKING_ASSIGN', 'Assign picking tasks to eligible users at assigned source locations.'),
       (gen_random_uuid(), 'PICKING_UPDATE',
        'Record picked quantities, report shortages, and update the progress of picking tasks assigned to the current user.'),
       (gen_random_uuid(), 'PICKING_SHORTAGE_APPROVE',
        'Approve reported picking shortages and the corresponding adjustments to transfer order quantities and stock reservations at assigned source locations.');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE (r.code = 'ADMIN' AND p.code IN ('PRODUCT_READ', 'PRODUCT_WRITE',
                                       'LOCATION_READ', 'LOCATION_MANAGE',
                                       'USER_MANAGE', 'ACCESS_MANAGE'
    ))
   OR (r.code IN ('WAREHOUSE_MANAGER', 'STORE_MANAGER') AND p.code IN ('PRODUCT_READ', 'LOCATION_READ',
                                                                       'INVENTORY_READ', 'INVENTORY_CREATE',
                                                                       'INVENTORY_CONFIGURE', 'INVENTORY_ADJUST',
                                                                       'TRANSFER_ORDER_READ', 'TRANSFER_ORDER_CREATE',
                                                                       'TRANSFER_ORDER_EDIT', 'TRANSFER_ORDER_SUBMIT',
                                                                       'TRANSFER_ORDER_DISPATCH',
                                                                       'TRANSFER_ORDER_RECEIVE',
                                                                       'TRANSFER_ORDER_CANCEL',
                                                                       'PICKING_READ', 'PICKING_ASSIGN',
                                                                       'PICKING_SHORTAGE_APPROVE'
    ))
   OR (r.code = 'VIEWER' AND p.code IN ('PRODUCT_READ', 'LOCATION_READ',
                                        'INVENTORY_READ', 'TRANSFER_ORDER_READ'
    ))
   OR (r.code = 'PICKER' AND p.code IN ('PRODUCT_READ', 'LOCATION_READ',
                                        'INVENTORY_READ', 'TRANSFER_ORDER_READ',
                                        'PICKING_READ', 'PICKING_UPDATE'
    ));