-- Module AccessManagement — le client B2B
-- UC-01 est hors périmètre : aucun compte n'est créé par l'API en Phase 1.
-- Le client de démonstration est amorcé par la migration V4 (§7.2).

CREATE SCHEMA IF NOT EXISTS access;

CREATE TABLE access.clients (
    client_id    varchar(32)  NOT NULL,
    company_name varchar(120) NOT NULL,

    CONSTRAINT pk_clients PRIMARY KEY (client_id)
);
