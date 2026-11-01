-- Drone Service — UC-02. État final des migrations V2 et V7 de la Phase 1.

CREATE SCHEMA IF NOT EXISTS drone;

CREATE TABLE drone.drones (
    drone_id      uuid         NOT NULL,
    -- Claim azp du jeton : une chaîne (inspectra-demo), et non plus un uuid comme en Phase 1.
    client_id     varchar(64)  NOT NULL,
    imsi          varchar(15)  NOT NULL,
    iccid         varchar(20)  NOT NULL,
    sim_type      varchar(8)   NOT NULL,
    status        varchar(16)  NOT NULL,
    registered_at timestamptz  NOT NULL,

    CONSTRAINT pk_drones PRIMARY KEY (drone_id),

    -- Double la vérification applicative : deux requêtes simultanées
    -- franchissent toutes deux existsByImsi, seule la contrainte les départage.
    CONSTRAINT uq_drones_imsi UNIQUE (imsi),
    -- Un support ne sert qu'un seul drone, comme l'IMSI.
    CONSTRAINT uq_drones_iccid UNIQUE (iccid),

    CONSTRAINT ck_drones_imsi CHECK (imsi ~ '^[0-9]{15}$'),
    -- UIT-T E.118 : préfixe 89, 19 ou 20 chiffres. Le chiffre de contrôle est vérifié par le domaine.
    CONSTRAINT ck_drones_iccid CHECK (iccid ~ '^89[0-9]{17,18}$'),
    CONSTRAINT ck_drones_sim_type CHECK (sim_type IN ('SIM', 'ESIM')),
    CONSTRAINT ck_drones_status CHECK (status IN ('REGISTERED', 'RETIRED'))
);

-- Toute lecture étant filtrée par client, l'index sert chaque requête.
CREATE INDEX idx_drones_client_id ON drone.drones (client_id);
