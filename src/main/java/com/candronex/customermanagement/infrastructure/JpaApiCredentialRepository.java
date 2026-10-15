package com.candronex.customermanagement.infrastructure;

import com.candronex.customermanagement.domain.ApiCredential;
import com.candronex.customermanagement.domain.port.ApiCredentialRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : réalise le port ApiCredentialRepository et traduit l'agrégat en lignes. */
@Repository
class JpaApiCredentialRepository implements ApiCredentialRepository {

    private final ApiCredentialEntityStore credentials;

    JpaApiCredentialRepository(ApiCredentialEntityStore credentials) {
        this.credentials = credentials;
    }

    @Override
    public Optional<ApiCredential> findByClientId(UUID clientId) {
        return credentials.findById(clientId).map(ApiCredentialEntity::toDomain);
    }

    @Override
    public void save(ApiCredential credential) {
        credentials.save(ApiCredentialEntity.from(credential));
    }
}
