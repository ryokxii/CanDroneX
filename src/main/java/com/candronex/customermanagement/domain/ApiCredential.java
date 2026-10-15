package com.candronex.customermanagement.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Racine d'agrégat : l'identifiant d'accès OAuth 2.0 d'un client, à raison d'un par client.
 *
 * <p>Distinct de Client parce que son cycle de vie l'est : il s'émet, expire et se révoque sans que
 * le client change. Seule l'empreinte BCrypt du secret est conservée.
 */
public final class ApiCredential {

    private final UUID clientId;
    private final String secretHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final Instant revokedAt;

    private ApiCredential(UUID clientId, String secretHash, Instant issuedAt,
                          Instant expiresAt, Instant revokedAt) {
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
        this.clientId = clientId;
        this.secretHash = secretHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    /** Délivre un identifiant à un client. */
    public static ApiCredential issue(UUID clientId, String secretHash,
                                      Instant issuedAt, Instant expiresAt) {
        return new ApiCredential(clientId, secretHash, issuedAt, expiresAt, null);
    }

    /** Reconstitue un identifiant déjà délivré ; réservé à l'adaptateur de persistance. */
    public static ApiCredential restore(UUID clientId, String secretHash, Instant issuedAt,
                                        Instant expiresAt, Instant revokedAt) {
        return new ApiCredential(clientId, secretHash, issuedAt, expiresAt, revokedAt);
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

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
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
        return clientId.equals(that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }

    /** L'empreinte ne doit jamais apparaître dans un journal. */
    @Override
    public String toString() {
        return "ApiCredential[clientId=" + clientId + "]";
    }
}
