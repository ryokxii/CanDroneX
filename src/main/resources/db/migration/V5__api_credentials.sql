-- Identifiants OAuth 2.0 des clients ; seule l'empreinte BCrypt du secret est stockée.

CREATE TABLE access.api_credentials (
    client_id   varchar(32)  NOT NULL,
    secret_hash varchar(100) NOT NULL,
    issued_at   timestamptz  NOT NULL,
    expires_at  timestamptz,
    revoked_at  timestamptz,

    CONSTRAINT pk_api_credentials PRIMARY KEY (client_id),
    CONSTRAINT fk_api_credentials_client
        FOREIGN KEY (client_id) REFERENCES access.clients (client_id),
    CONSTRAINT ck_api_credentials_expiry
        CHECK (expires_at IS NULL OR expires_at > issued_at)
);

-- Démonstration uniquement — CLI-INSPECTRA / inspectra-demo-secret
INSERT INTO access.api_credentials (client_id, secret_hash, issued_at)
VALUES ('CLI-INSPECTRA',
        '$2a$10$s3j6wjnT3siApTQ5ThhLKOc4hT9Bcv5o.Q/lfdWRufzOmUYluDQfa',
        now())
ON CONFLICT (client_id) DO NOTHING;
