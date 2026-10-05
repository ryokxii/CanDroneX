package com.candronex.accessmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Identifiants OAuth 2.0 d'un client ; seule l'empreinte BCrypt du secret est conservée. */
@Entity
@Table(schema = "access", name = "api_credentials")
public class ApiCredential {

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

    /** Requis par l'ORM ; jamais appelé par le domaine. */
    protected ApiCredential() {
    }

    private ApiCredential(UUID clientId, String secretHash, Instant issuedAt,
                          Instant expiresAt, Instant revokedAt) {
        this.clientId = clientId;
        this.secretHash = secretHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    /** Délivre un identifiant à un client. */
    public static ApiCredential issue(UUID clientId, String secretHash,
                                      Instant issuedAt, Instant expiresAt) {
        if (clientId == null) {
            throw new IllegalArgumentException("L'identifiant du client est obligatoire.");
        }
        if (secretHash == null || secretHash.isBlank()) {
            throw new IllegalArgumentException("L'empreinte du secret est obligatoire.");
        }
        Objects.requireNonNull(issuedAt, "issuedAt");
        if (expiresAt != null && !expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("L'échéance doit suivre la date d'émission.");
        }
        return new ApiCredential(clientId, secretHash, issuedAt, expiresAt, null);
    }

    /** Retourne une copie révoquée ; l'original reste inchangé. */
    public ApiCredential revokedAt(Instant when) {
        Objects.requireNonNull(when, "when");
        if (revokedAt != null) {
            return this;
        }
        return new ApiCredential(clientId, secretHash, issuedAt, expiresAt, when);
    }

    /**
     * @return vrai si l'identifiant permet d'obtenir un jeton à cet instant
     */
    public boolean isUsableAt(Instant now) {
        if (revokedAt != null && !now.isBefore(revokedAt)) {
            return false;
        }
        return expiresAt == null || now.isBefore(expiresAt);
    }

    public UUID clientId() {
        return clientId;
    }

    public String secretHash() {
        return secretHash;
    }

    /** Égalité par identité. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ApiCredential that)) {
            return false;
        }
        return Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
