package com.candronex.accessmanagement.application;

import com.candronex.accessmanagement.domain.port.ClientRepository;
import com.candronex.accessmanagement.published.ClientDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Réalise l'interface publiée ClientDirectory. */
@Service
class ClientDirectoryService implements ClientDirectory {

    private final ClientRepository clients;

    ClientDirectoryService(ClientRepository clients) {
        this.clients = clients;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String clientId) {
        return clientId != null && clients.existsById(clientId);
    }
}
