package com.candronex.accessmanagement.domain.port;

import com.candronex.accessmanagement.domain.Client;

import java.util.Optional;

/** Port sortant de l'agrégat Client. */
public interface ClientRepository {

    Optional<Client> findById(String clientId);

    boolean existsById(String clientId);

    void save(Client client);
}
