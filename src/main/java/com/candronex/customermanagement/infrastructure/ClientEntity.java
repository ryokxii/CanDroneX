package com.candronex.customermanagement.infrastructure;

import com.candronex.customermanagement.domain.Client;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Correspondance relationnelle de l'agrégat Client ; ne sort jamais de l'adaptateur. */
@Entity
@Table(schema = "customer", name = "clients")
class ClientEntity {

    @Id
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "company_name", nullable = false, length = 120)
    private String companyName;

    /** Requis par l'ORM. */
    protected ClientEntity() {
    }

    static ClientEntity from(Client client) {
        ClientEntity entity = new ClientEntity();
        entity.clientId = client.clientId();
        entity.companyName = client.companyName();
        return entity;
    }

    Client toDomain() {
        return Client.restore(clientId, companyName);
    }
}
