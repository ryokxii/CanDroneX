package com.candronex.accessmanagement.domain.port;

import com.candronex.accessmanagement.domain.ApiCredential;

import java.util.Optional;

/** Port sortant de l'entité ApiCredential. */
public interface ApiCredentialRepository {

    Optional<ApiCredential> findByClientId(String clientId);

    void save(ApiCredential credential);
}
