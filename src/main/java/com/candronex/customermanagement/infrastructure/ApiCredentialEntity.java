package com.candronex.customermanagement.infrastructure;

import com.candronex.customermanagement.domain.ApiCredential;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Correspondance relationnelle de l'agrégat ApiCredential ; ne sort jamais de l'adaptateur. */
@Entity
@Table(schema = "customer", name = "api_credentials")
class ApiCredentialEntity {

    @Id
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "secret_hash", nullable = false, length = 100)
    private String secretHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Requis par l'ORM. */
    protected ApiCredentialEntity() {
    }

    static ApiCredentialEntity from(ApiCredential credential) {
        ApiCredentialEntity entity = new ApiCredentialEntity();
        entity.clientId = credential.clientId();
        entity.secretHash = credential.secretHash();
        entity.issuedAt = credential.issuedAt();
        entity.expiresAt = credential.expiresAt();
        entity.revokedAt = credential.revokedAt();
        return entity;
    }

    ApiCredential toDomain() {
        return ApiCredential.restore(clientId, secretHash, issuedAt, expiresAt, revokedAt);
    }
}
