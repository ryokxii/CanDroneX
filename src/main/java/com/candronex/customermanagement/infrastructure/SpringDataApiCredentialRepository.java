package com.candronex.customermanagement.infrastructure;

import com.candronex.customermanagement.domain.ApiCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Interface Spring Data, détail d'infrastructure — portée paquet. */
interface SpringDataApiCredentialRepository extends JpaRepository<ApiCredential, UUID> {
}
