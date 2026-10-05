package com.candronex.accessmanagement.infrastructure;

import com.candronex.accessmanagement.domain.Client;
import com.candronex.accessmanagement.domain.port.ClientRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : réalise le port ClientRepository avec l'ORM. */
@Repository
class JpaClientRepository implements ClientRepository {

    private final SpringDataClientRepository clients;

    JpaClientRepository(SpringDataClientRepository clients) {
        this.clients = clients;
    }

    @Override
    public Optional<Client> findById(UUID clientId) {
        return clients.findById(clientId);
    }

    @Override
    public boolean existsById(UUID clientId) {
        return clients.existsById(clientId);
    }

    @Override
    public void save(Client client) {
        clients.save(client);
    }
}
