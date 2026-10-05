-- Module DroneRegistration — UC-02

CREATE SCHEMA IF NOT EXISTS drone;

CREATE TABLE drone.drones (
    drone_id      uuid         NOT NULL,
    client_id     uuid         NOT NULL,
    imsi          varchar(15)  NOT NULL,
    sim_type      varchar(8)   NOT NULL,
    status        varchar(16)  NOT NULL,
    created_at    timestamptz  NOT NULL,

    CONSTRAINT pk_drones PRIMARY KEY (drone_id),

    -- Double la vérification applicative : deux requêtes simultanées
    -- franchissent toutes deux existsByImsi, seule la contrainte les départage.
    CONSTRAINT uq_drones_imsi UNIQUE (imsi),

    CONSTRAINT ck_drones_imsi CHECK (imsi ~ '^[0-9]{15}$'),
    CONSTRAINT ck_drones_sim_type CHECK (sim_type IN ('SIM', 'ESIM')),
    CONSTRAINT ck_drones_status CHECK (status IN ('REGISTERED', 'RETIRED'))
);

-- client_id est un identifiant nu : le client appartient à un autre module.
-- Toute lecture étant filtrée par client, l'index sert chaque requête.
CREATE INDEX idx_drones_client_id ON drone.drones (client_id);
