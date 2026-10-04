package com.candronex.accessmanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Racine d'agrégat du contexte AccessManagement : l'exploitant de drones abonné à la plateforme.
 */
@Entity
@Table(schema = "access", name = "clients")
public class Client {

    @Id
    @Column(name = "client_id", nullable = false, length = 32)
    private String clientId;

    @Column(name = "company_name", nullable = false, length = 120)
    private String companyName;


    /** Requis par l'ORM ; jamais appelé par le domaine. */
    protected Client() {
    }

    private Client(String clientId, String companyName) {
        this.clientId = clientId;
        this.companyName = companyName;
    }

    /** Crée un client B2B. */
    public static Client onboard(String clientId, String companyName) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("L'identifiant du client est obligatoire.");
        }
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("La raison sociale est obligatoire.");
        }
        return new Client(clientId, companyName);
    }

    public String clientId() {
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
        return Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
