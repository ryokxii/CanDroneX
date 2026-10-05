-- Module AccessManagement — le client B2B

CREATE SCHEMA IF NOT EXISTS access;

CREATE TABLE access.clients (
    client_id    uuid         NOT NULL,
    company_name varchar(120) NOT NULL,

    CONSTRAINT pk_clients PRIMARY KEY (client_id)
);
