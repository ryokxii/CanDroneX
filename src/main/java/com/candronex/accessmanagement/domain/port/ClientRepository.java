package com.candronex.accessmanagement.domain.port;

import com.candronex.accessmanagement.domain.Client;

import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'agrégat Client. */
public interface ClientRepository {

    Optional<Client> findById(UUID clientId);

    boolean existsById(UUID clientId);

    void save(Client client);
}
