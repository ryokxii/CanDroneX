-- Module OrderManagement — UC-04
-- Le schéma est nommé « orders » et non « order » : ORDER est un mot
-- réservé SQL, qui imposerait des guillemets dans chaque requête.

CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE orders.service_orders (
    order_id        varchar(32)  NOT NULL,
    client_id       varchar(32)  NOT NULL,
    idempotency_key varchar(64)  NOT NULL,
    status          varchar(24)  NOT NULL,
    placed_at       timestamptz  NOT NULL,
    completed_at    timestamptz,

    CONSTRAINT pk_service_orders PRIMARY KEY (order_id),

    -- §8.2 : traite le rejeu simultané, que la lecture en début
    -- de transaction ne peut pas intercepter.
    CONSTRAINT uq_orders_client_idempotency UNIQUE (client_id, idempotency_key),

    CONSTRAINT ck_orders_status CHECK (
        status IN ('RECEIVED', 'IN_PROGRESS', 'COMPLETED',
                   'PARTIALLY_COMPLETED', 'FAILED', 'CANCELLED')
    )
);

CREATE INDEX idx_service_orders_client_id ON orders.service_orders (client_id);

CREATE TABLE orders.order_lines (
    order_line_id varchar(32)  NOT NULL,
    order_id      varchar(32)  NOT NULL,
    drone_id      varchar(32)  NOT NULL,
    service_type  varchar(24)  NOT NULL,
    status        varchar(24)  NOT NULL,

    CONSTRAINT pk_order_lines PRIMARY KEY (order_line_id),

    -- Seule clé étrangère du modèle : elle reste à l'intérieur de l'agrégat.
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id)
        REFERENCES orders.service_orders (order_id) ON DELETE CASCADE,

    -- Invariant de l'agrégat Order, doublé en base.
    CONSTRAINT uq_order_lines_drone_service UNIQUE (order_id, drone_id, service_type),

    CONSTRAINT ck_order_lines_service_type CHECK (
        service_type IN ('C2_URLLC', 'IMAGERY_EMBB')
    ),
    CONSTRAINT ck_order_lines_status CHECK (
        status IN ('RECEIVED', 'IN_PROGRESS', 'COMPLETED',
                   'PARTIALLY_COMPLETED', 'FAILED', 'CANCELLED')
    )
);

-- drone_id est un identifiant nu : le drone appartient à DroneRegistration.
CREATE INDEX idx_order_lines_drone_id ON orders.order_lines (drone_id);
