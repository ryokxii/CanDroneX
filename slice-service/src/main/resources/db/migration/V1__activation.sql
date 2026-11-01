-- Slice Service — Service Activation (TMF640).

CREATE SCHEMA IF NOT EXISTS activation;

CREATE TABLE activation.services (
    service_id    uuid         NOT NULL,
    -- Référence par identifiant : la ligne appartient à Order Service, dans une autre base.
    order_line_id uuid         NOT NULL,
    drone_id      uuid         NOT NULL,
    state         varchar(16)  NOT NULL,

    CONSTRAINT pk_activation_services PRIMARY KEY (service_id),
    -- Une seule activation par ligne : une reprise avec le même orderLineId ne crée pas de doublon.
    CONSTRAINT uq_activation_services_order_line UNIQUE (order_line_id),
    -- UNCONFIRMED : délai dépassé, issue inconnue ; ce n'est pas un échec.
    CONSTRAINT ck_activation_services_state CHECK (
        state IN ('PENDING', 'ACTIVE', 'FAILED', 'UNCONFIRMED')
    )
);

-- Les caractéristiques reçues dans la demande d'activation : Slice Service ne lit jamais order_db.
CREATE TABLE activation.service_characteristics (
    service_id uuid        NOT NULL,
    position   smallint    NOT NULL,
    name       varchar(32) NOT NULL,
    value      varchar(64) NOT NULL,

    CONSTRAINT pk_service_characteristics PRIMARY KEY (service_id, name),
    CONSTRAINT fk_service_characteristics_service FOREIGN KEY (service_id)
        REFERENCES activation.services (service_id) ON DELETE CASCADE
);

-- Une ligne par appel au réseau ; accepted_at et activated_at servent au calcul du P95.
CREATE TABLE activation.activation_attempts (
    attempt_id   uuid        NOT NULL,
    service_id   uuid        NOT NULL,
    accepted_at  timestamptz NOT NULL,
    activated_at timestamptz,
    -- NULL tant que le réseau n'a pas répondu ; UNKNOWN : délai dépassé.
    result       varchar(8),

    CONSTRAINT pk_activation_attempts PRIMARY KEY (attempt_id),
    CONSTRAINT fk_activation_attempts_service FOREIGN KEY (service_id)
        REFERENCES activation.services (service_id) ON DELETE CASCADE,
    CONSTRAINT ck_activation_attempts_result CHECK (result IN ('SUCCESS', 'FAILED', 'UNKNOWN')),
    CONSTRAINT ck_activation_attempts_activated_success
        CHECK (activated_at IS NULL OR result = 'SUCCESS'),
    CONSTRAINT ck_activation_attempts_activated_after
        CHECK (activated_at IS NULL OR activated_at >= accepted_at)
);

CREATE INDEX idx_activation_attempts_service_id ON activation.activation_attempts (service_id);
