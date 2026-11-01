-- Order Service — jeu de données de démonstration.
-- Chargé seulement avec le profil demo (spring.flyway.locations inclut classpath:db/demo).
-- Le catalogue n'est pas ici : c'est une donnée de référence, insérée par V1__catalog.sql.
-- drone_id reprend les drones de drone_db ; order_line_id est repris dans slice_db.
-- Une commande par état utile : PARTIAL, COMPLETED, IN_PROGRESS et RECEIVED.

INSERT INTO orders.service_orders
    (order_id, client_id, idempotency_key, status, placed_at, completed_at) VALUES
    -- C2 actif + Imagerie en échec → PARTIAL (règle 5 d'OrderStatusPolicy).
    ('a0000000-0000-4000-8000-000000000001', 'inspectra-demo', 'demo-commande-0001',
     'PARTIAL',     '2026-10-01 14:05:00+00', '2026-10-01 14:05:03+00'),
    -- Toutes les lignes COMPLETED → COMPLETED.
    ('a0000000-0000-4000-8000-000000000002', 'inspectra-demo', 'demo-commande-0002',
     'COMPLETED',   '2026-10-01 14:10:00+00', '2026-10-01 14:10:02+00'),
    -- Service UNCONFIRMED (délai dépassé) → ligne IN_PROGRESS → commande IN_PROGRESS.
    ('a0000000-0000-4000-8000-000000000003', 'aerotech-demo',  'demo-commande-0003',
     'IN_PROGRESS', '2026-10-01 14:15:00+00', NULL),
    -- Slice Service était indisponible : la ligne reste RECEIVED, ActivationStatusPoller la reprend (UC-10).
    ('a0000000-0000-4000-8000-000000000004', 'inspectra-demo', 'demo-commande-0004',
     'RECEIVED',    '2026-10-01 14:20:00+00', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO orders.order_lines
    (order_line_id, order_id, drone_id, service_specification_id, position, status) VALUES
    ('b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001',
     'd0000000-0000-4000-8000-000000000001', 'C2_URLLC',     0, 'COMPLETED'),
    ('b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001',
     'd0000000-0000-4000-8000-000000000001', 'IMAGERY_EMBB', 1, 'FAILED'),
    ('b0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000002',
     'd0000000-0000-4000-8000-000000000002', 'C2_URLLC',     0, 'COMPLETED'),
    ('b0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000003',
     'd0000000-0000-4000-8000-000000000004', 'IMAGERY_EMBB', 0, 'IN_PROGRESS'),
    ('b0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000004',
     'd0000000-0000-4000-8000-000000000002', 'IMAGERY_EMBB', 0, 'RECEIVED')
ON CONFLICT DO NOTHING;

-- Caractéristiques figées : copie, à la soumission, des valeurs de V1__catalog.sql.
INSERT INTO orders.order_line_characteristics (order_line_id, position, name, value)
SELECT line.order_line_id, spec.position, spec.name, spec.default_value
FROM orders.order_lines AS line
JOIN catalog.characteristic_specifications AS spec
    ON spec.service_specification_id = line.service_specification_id
WHERE line.order_line_id IN (
    'b0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000002',
    'b0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000004',
    'b0000000-0000-4000-8000-000000000005')
ON CONFLICT DO NOTHING;
