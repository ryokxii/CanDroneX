-- Données de démonstration
--
-- Le strict préalable au scénario de bout en bout, et rien de plus :
-- UC-01 étant hors périmètre, aucune route ne crée de client, donc il
-- doit préexister. Aucun drone ni commande n'est amorcé — ce sont
-- précisément UC-02 et UC-04 que la démonstration doit produire.
--
-- Le catalogue n'a rien à amorcer : l'offre est figée dans le module
-- ServiceCatalog, pas en base.

INSERT INTO access.clients (client_id, company_name)
VALUES ('CLI-INSPECTRA', 'Inspectra Drone Services')
ON CONFLICT (client_id) DO NOTHING;
