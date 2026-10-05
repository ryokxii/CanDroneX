package com.candronex.accessmanagement;

import com.candronex.accessmanagement.domain.ApiCredential;
import com.candronex.accessmanagement.domain.port.ApiCredentialRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Réalisation en mémoire du port ApiCredentialRepository. */
public class InMemoryApiCredentialRepository implements ApiCredentialRepository {

    private final Map<UUID, ApiCredential> byClientId = new ConcurrentHashMap<>();

    @Override
    public Optional<ApiCredential> findByClientId(UUID clientId) {
        return Optional.ofNullable(byClientId.get(clientId));
    }

    @Override
    public void save(ApiCredential credential) {
        byClientId.put(credential.clientId(), credential);
    }
}
