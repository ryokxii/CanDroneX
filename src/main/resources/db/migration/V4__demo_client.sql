-- Données de démonstration

INSERT INTO access.clients (client_id, company_name)
VALUES ('0b6f1c2e-8f4a-4d3b-9c71-5e2a7d9f3b10', 'Inspectra Drone Services')
ON CONFLICT (client_id) DO NOTHING;
