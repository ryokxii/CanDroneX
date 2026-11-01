-- Drone Service — jeu de données de démonstration.
-- Chargé seulement avec le profil demo (spring.flyway.locations inclut classpath:db/demo).
-- Les clients sont ceux du realm Keycloak : client_id reçoit la claim azp, aucun secret ici.
-- Les drone_id sont repris tels quels dans order_db et slice_db (référence par identifiant).

INSERT INTO drone.drones (drone_id, client_id, imsi, iccid, sim_type, status, registered_at) VALUES
    -- Drone de la commande PARTIAL (C2 actif, Imagerie en échec).
    ('d0000000-0000-4000-8000-000000000001', 'inspectra-demo',
     '302720123456789', '8999700000000010239', 'ESIM', 'REGISTERED', '2026-10-01 13:00:00+00'),
    -- Drone de la commande COMPLETED et de la commande restée RECEIVED.
    ('d0000000-0000-4000-8000-000000000002', 'inspectra-demo',
     '302720123456790', '8999700000000010247', 'SIM',  'REGISTERED', '2026-10-01 13:10:00+00'),
    -- Drone retiré : une commande sur ce drone est refusée.
    ('d0000000-0000-4000-8000-000000000003', 'inspectra-demo',
     '302720123456791', '8999700000000010254', 'ESIM', 'RETIRED',    '2026-10-01 13:20:00+00'),
    -- Drone d'un autre client : inspectra-demo reçoit 404 en le demandant.
    ('d0000000-0000-4000-8000-000000000004', 'aerotech-demo',
     '302720987654321', '8999700000000010262', 'ESIM', 'REGISTERED', '2026-10-01 13:30:00+00')
ON CONFLICT DO NOTHING;
