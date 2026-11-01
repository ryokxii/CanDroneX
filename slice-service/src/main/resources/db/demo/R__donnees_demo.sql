-- Slice Service — jeu de données de démonstration.
-- Chargé seulement avec le profil demo (spring.flyway.locations inclut classpath:db/demo).
-- order_line_id et drone_id reprennent order_db et drone_db (référence par identifiant).
-- La ligne b…05, restée RECEIVED dans order_db, n'a volontairement aucun service ici.

INSERT INTO activation.services (service_id, order_line_id, drone_id, state) VALUES
    -- Commande PARTIAL : C2 actif, Imagerie en échec.
    ('c0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001',
     'd0000000-0000-4000-8000-000000000001', 'ACTIVE'),
    ('c0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002',
     'd0000000-0000-4000-8000-000000000001', 'FAILED'),
    -- Commande COMPLETED.
    ('c0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000003',
     'd0000000-0000-4000-8000-000000000002', 'ACTIVE'),
    -- Commande IN_PROGRESS : délai dépassé, findStatus sera appelé avant toute reprise.
    ('c0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000004',
     'd0000000-0000-4000-8000-000000000004', 'UNCONFIRMED')
ON CONFLICT DO NOTHING;

-- Caractéristiques reçues dans la demande d'activation, identiques à celles figées dans order_db.
INSERT INTO activation.service_characteristics (service_id, position, name, value)
SELECT s.service_id, c.position, c.name, c.value
FROM (VALUES
    ('c0000000-0000-4000-8000-000000000001'::uuid, 'C2'),
    ('c0000000-0000-4000-8000-000000000002'::uuid, 'IMAGERY'),
    ('c0000000-0000-4000-8000-000000000003'::uuid, 'C2'),
    ('c0000000-0000-4000-8000-000000000004'::uuid, 'IMAGERY')
) AS s (service_id, offer)
JOIN (VALUES
    ('C2', 0, 'sst', '2'),      ('C2', 1, 'sd', '000001'),      ('C2', 2, 'dnn', 'c2'),
    ('C2', 3, 'fiveQi', '7'),   ('C2', 4, 'arp', '2'),
    ('C2', 5, 'ambrUplink', '20 Mbps'),       ('C2', 6, 'ambrDownlink', '20 Mbps'),
    ('IMAGERY', 0, 'sst', '1'), ('IMAGERY', 1, 'sd', '000002'), ('IMAGERY', 2, 'dnn', 'imagery'),
    ('IMAGERY', 3, 'fiveQi', '9'), ('IMAGERY', 4, 'arp', '8'),
    ('IMAGERY', 5, 'ambrUplink', '500 Mbps'), ('IMAGERY', 6, 'ambrDownlink', '100 Mbps')
) AS c (offer, position, name, value) ON c.offer = s.offer
ON CONFLICT DO NOTHING;

INSERT INTO activation.activation_attempts
    (attempt_id, service_id, accepted_at, activated_at, result) VALUES
    ('e0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001',
     '2026-10-01 14:05:00.120+00', '2026-10-01 14:05:01.480+00', 'SUCCESS'),
    ('e0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000002',
     '2026-10-01 14:05:00.150+00', NULL,                         'FAILED'),
    ('e0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000003',
     '2026-10-01 14:10:00.110+00', '2026-10-01 14:10:01.270+00', 'SUCCESS'),
    -- MOCK_TIMEOUT = 5 s dépassé : issue inconnue, pas d'activated_at.
    ('e0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000004',
     '2026-10-01 14:15:00.130+00', NULL,                         'UNKNOWN')
ON CONFLICT DO NOTHING;

INSERT INTO inventory.services (service_id, drone_id, state, updated_at) VALUES
    ('c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001',
     'ACTIVE',      '2026-10-01 14:05:01.480+00'),
    ('c0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000001',
     'FAILED',      '2026-10-01 14:05:01.900+00'),
    ('c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000002',
     'ACTIVE',      '2026-10-01 14:10:01.270+00'),
    ('c0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000004',
     'UNCONFIRMED', '2026-10-01 14:15:05.130+00')
ON CONFLICT DO NOTHING;
