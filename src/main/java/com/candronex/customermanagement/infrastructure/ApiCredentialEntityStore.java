package com.candronex.customermanagement.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Accès Spring Data aux lignes de customer.api_credentials ; détail interne de JpaApiCredentialRepository. */
interface ApiCredentialEntityStore extends JpaRepository<ApiCredentialEntity, UUID> {
}
