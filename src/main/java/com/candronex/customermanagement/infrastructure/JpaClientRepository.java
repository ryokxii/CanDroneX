package com.candronex.customermanagement.infrastructure;

import com.candronex.customermanagement.domain.Client;
import com.candronex.customermanagement.domain.port.ClientRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : réalise le port ClientRepository et traduit l'agrégat en lignes. */
@Repository
class JpaClientRepository implements ClientRepository {

    private final ClientEntityStore clients;

    JpaClientRepository(ClientEntityStore clients) {
        this.clients = clients;
    }

    @Override
    public Optional<Client> findById(UUID clientId) {
        return clients.findById(clientId).map(ClientEntity::toDomain);
    }

    @Override
    public boolean existsById(UUID clientId) {
        return clients.existsById(clientId);
    }

    @Override
    public void save(Client client) {
        clients.save(ClientEntity.from(client));
    }
}
