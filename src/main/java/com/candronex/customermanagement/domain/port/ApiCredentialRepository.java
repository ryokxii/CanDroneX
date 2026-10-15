package com.candronex.customermanagement.domain.port;

import com.candronex.customermanagement.domain.ApiCredential;

import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'entité ApiCredential. */
public interface ApiCredentialRepository {

    Optional<ApiCredential> findByClientId(UUID clientId);

    void save(ApiCredential credential);
}
