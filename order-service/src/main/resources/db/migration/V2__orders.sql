-- Order Service — Order Management (TMF641). État final des migrations V3, V9 et V10 de la Phase 1.

CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE orders.service_orders (
    order_id        uuid         NOT NULL,
    -- Claim azp du jeton : une chaîne, et non plus un uuid comme en Phase 1.
    client_id       varchar(64)  NOT NULL,
    idempotency_key varchar(64)  NOT NULL,
    status          varchar(24)  NOT NULL,
    placed_at       timestamptz  NOT NULL,
    completed_at    timestamptz,

    CONSTRAINT pk_service_orders PRIMARY KEY (order_id),

    -- Traite le rejeu simultané, que la lecture en début
    -- de transaction ne peut pas intercepter.
    CONSTRAINT uq_orders_client_idempotency UNIQUE (client_id, idempotency_key),

    -- TMF641 : partial.
    CONSTRAINT ck_orders_status CHECK (
        status IN ('RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'PARTIAL', 'FAILED', 'CANCELLED')
    )
);

CREATE INDEX idx_service_orders_client_id ON orders.service_orders (client_id);

CREATE TABLE orders.order_lines (
    order_line_id                 uuid         NOT NULL,
    order_id                      uuid         NOT NULL,
    -- Référence par identifiant : le drone appartient à Drone Service, dans une autre base.
    drone_id                      uuid         NOT NULL,
    -- Ni CHECK sur la liste des offres ni clé étrangère vers catalog :
    -- le catalogue reste seul propriétaire de l'offre.
    service_specification_id      varchar(32)  NOT NULL,
    -- Rang de la ligne dans sa commande : la réponse restitue l'ordre de la requête.
    position                      smallint     NOT NULL,
    status                        varchar(24)  NOT NULL,

    CONSTRAINT pk_order_lines PRIMARY KEY (order_line_id),

    -- Reste à l'intérieur de l'agrégat Order.
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id)
        REFERENCES orders.service_orders (order_id) ON DELETE CASCADE,

    -- Invariant de l'agrégat Order, doublé en base.
    CONSTRAINT uq_order_lines_drone_specification
        UNIQUE (order_id, drone_id, service_specification_id),
    CONSTRAINT uq_order_lines_position UNIQUE (order_id, position),

    -- PARTIAL ne s'applique qu'à la commande.
    CONSTRAINT ck_order_lines_status CHECK (
        status IN ('RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')
    )
);

CREATE INDEX idx_order_lines_drone_id ON orders.order_lines (drone_id);

-- Les caractéristiques commandées, copiées du catalogue à la soumission et jamais modifiées :
-- une évolution de l'offre ne réécrit pas ce qui a été vendu.
CREATE TABLE orders.order_line_characteristics (
    order_line_id uuid        NOT NULL,
    position      smallint    NOT NULL,
    name          varchar(32) NOT NULL,
    value         varchar(64) NOT NULL,

    CONSTRAINT pk_order_line_characteristics PRIMARY KEY (order_line_id, name),
    CONSTRAINT fk_order_line_characteristics_line FOREIGN KEY (order_line_id)
        REFERENCES orders.order_lines (order_line_id) ON DELETE CASCADE
);
