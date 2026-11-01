-- Slice Service — Service Inventory (TMF638) : l'état courant de chaque service, par drone.

CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.services (
    -- Même identifiant que activation.services, sans clé étrangère : deux contextes distincts,
    -- mis à jour dans la même transaction par RecordServiceStateUseCase.
    service_id uuid        NOT NULL,
    drone_id   uuid        NOT NULL,
    state      varchar(16) NOT NULL,
    updated_at timestamptz NOT NULL,

    CONSTRAINT pk_inventory_services PRIMARY KEY (service_id),
    CONSTRAINT ck_inventory_services_state CHECK (
        state IN ('PENDING', 'ACTIVE', 'FAILED', 'UNCONFIRMED')
    )
);

CREATE INDEX idx_inventory_services_drone_id ON inventory.services (drone_id);
