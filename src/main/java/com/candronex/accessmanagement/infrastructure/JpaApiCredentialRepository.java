package com.candronex.accessmanagement.infrastructure;

import com.candronex.accessmanagement.domain.ApiCredential;
import com.candronex.accessmanagement.domain.port.ApiCredentialRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : réalise le port ApiCredentialRepository avec l'ORM. */
@Repository
class JpaApiCredentialRepository implements ApiCredentialRepository {

    private final SpringDataApiCredentialRepository credentials;

    JpaApiCredentialRepository(SpringDataApiCredentialRepository credentials) {
        this.credentials = credentials;
    }

    @Override
    public Optional<ApiCredential> findByClientId(UUID clientId) {
        return credentials.findById(clientId);
    }

    @Override
    public void save(ApiCredential credential) {
        credentials.save(credential);
    }
}
