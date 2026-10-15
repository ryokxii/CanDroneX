-- Module ServiceCatalog — TMF633 : ce qui peut être commandé, et avec quelles caractéristiques.
-- L'offre est une donnée : la faire évoluer est une migration, jamais un changement de code.

CREATE SCHEMA IF NOT EXISTS catalog;

CREATE TABLE catalog.service_specifications (
    service_specification_id varchar(32) NOT NULL,
    name                     varchar(80) NOT NULL,
    -- Incrémentée à chaque changement de l'offre ; la commande fige celle qu'elle a reçue.
    version                  integer     NOT NULL,

    CONSTRAINT pk_service_specifications PRIMARY KEY (service_specification_id),
    CONSTRAINT ck_service_specifications_id
        CHECK (service_specification_id ~ '^[A-Z][A-Z0-9_]{0,31}$'),
    CONSTRAINT ck_service_specifications_version CHECK (version > 0)
);

-- Valeurs possibles d'une caractéristique ; en Phase 1 et 2, une seule valeur par défaut,
-- que la commande copie. Les valeurs commandées vivent dans orders, pas ici.
CREATE TABLE catalog.characteristic_specifications (
    service_specification_id varchar(32) NOT NULL,
    position                 smallint    NOT NULL,
    name                     varchar(32) NOT NULL,
    value_type               varchar(16) NOT NULL,
    default_value            varchar(64) NOT NULL,

    CONSTRAINT pk_characteristic_specifications PRIMARY KEY (service_specification_id, name),
    CONSTRAINT fk_characteristic_specifications_specification
        FOREIGN KEY (service_specification_id)
        REFERENCES catalog.service_specifications (service_specification_id) ON DELETE CASCADE,
    CONSTRAINT ck_characteristic_specifications_value_type
        CHECK (value_type IN ('INTEGER', 'STRING'))
);

-- Commande de référence du dossier de domaine (§7.4 et §7.5) : ces valeurs font foi.
INSERT INTO catalog.service_specifications (service_specification_id, name, version) VALUES
    ('C2_URLLC',     'C&C Connectivity',     1),
    ('IMAGERY_EMBB', 'Imagery Connectivity', 1);

INSERT INTO catalog.characteristic_specifications
    (service_specification_id, position, name, value_type, default_value) VALUES
    ('C2_URLLC', 0, 'sst',          'INTEGER', '2'),
    ('C2_URLLC', 1, 'sd',           'STRING',  '000001'),
    ('C2_URLLC', 2, 'dnn',          'STRING',  'c2'),
    ('C2_URLLC', 3, 'fiveQi',       'INTEGER', '7'),
    ('C2_URLLC', 4, 'arp',          'INTEGER', '2'),
    ('C2_URLLC', 5, 'ambrUplink',   'STRING',  '20 Mbps'),
    ('C2_URLLC', 6, 'ambrDownlink', 'STRING',  '20 Mbps'),

    ('IMAGERY_EMBB', 0, 'sst',          'INTEGER', '1'),
    ('IMAGERY_EMBB', 1, 'sd',           'STRING',  '000002'),
    ('IMAGERY_EMBB', 2, 'dnn',          'STRING',  'imagery'),
    ('IMAGERY_EMBB', 3, 'fiveQi',       'INTEGER', '9'),
    ('IMAGERY_EMBB', 4, 'arp',          'INTEGER', '8'),
    ('IMAGERY_EMBB', 5, 'ambrUplink',   'STRING',  '500 Mbps'),
    ('IMAGERY_EMBB', 6, 'ambrDownlink', 'STRING',  '100 Mbps');
