package com.candronex.accessmanagement.domain.port;

import com.candronex.accessmanagement.domain.ApiCredential;

import java.util.Optional;
import java.util.UUID;

/** Port sortant de l'entité ApiCredential. */
public interface ApiCredentialRepository {

    Optional<ApiCredential> findByClientId(UUID clientId);

    void save(ApiCredential credential);
}
