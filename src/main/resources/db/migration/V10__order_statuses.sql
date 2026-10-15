-- Module OrderManagement — les états de la commande et ceux de ses lignes se distinguent.

-- 1. États de la commande : PARTIALLY_COMPLETED devient PARTIAL (TMF641 : partial).
ALTER TABLE orders.service_orders DROP CONSTRAINT ck_orders_status;

UPDATE orders.service_orders SET status = 'PARTIAL' WHERE status = 'PARTIALLY_COMPLETED';

ALTER TABLE orders.service_orders ADD CONSTRAINT ck_orders_status CHECK (
    status IN ('RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'PARTIAL', 'FAILED', 'CANCELLED')
);

-- 2. États d'une ligne : PARTIAL ne s'applique qu'à la commande.
ALTER TABLE orders.order_lines DROP CONSTRAINT ck_order_lines_status;

ALTER TABLE orders.order_lines ADD CONSTRAINT ck_order_lines_status CHECK (
    status IN ('RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')
);
