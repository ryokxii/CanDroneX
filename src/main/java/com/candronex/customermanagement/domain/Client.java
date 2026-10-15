package com.candronex.customermanagement.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Racine d'agrégat du contexte CustomerManagement : l'exploitant de drones abonné à la plateforme.
 *
 * <p>Le client est identifié par son clientId, généré par la plateforme. Cet identifiant est aussi
 * le client_id OAuth 2.0 de son ApiCredential et le sujet de ses jetons d'accès.
 */
public final class Client {

    private final UUID clientId;
    private final String companyName;

    private Client(UUID clientId, String companyName) {
        if (clientId == null) {
            throw new IllegalArgumentException("L'identifiant du client est obligatoire.");
        }
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("La raison sociale est obligatoire.");
        }
        this.clientId = clientId;
        this.companyName = companyName;
    }

    /** Crée un client B2B ; son identifiant est généré, jamais fourni (UC-01). */
    public static Client onboard(String companyName) {
        return new Client(UUID.randomUUID(), companyName);
    }

    /** Reconstitue un client déjà enregistré ; réservé à l'adaptateur de persistance. */
    public static Client restore(UUID clientId, String companyName) {
        return new Client(clientId, companyName);
    }

    public UUID clientId() {
        return clientId;
    }

    public String companyName() {
        return companyName;
    }

    /** Égalité par identité. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Client that)) {
            return false;
        }
        return clientId.equals(that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
