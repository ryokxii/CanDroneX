package com.candronex.customermanagement.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Accès Spring Data aux lignes de customer.clients ; détail interne de JpaClientRepository. */
interface ClientEntityStore extends JpaRepository<ClientEntity, UUID> {
}
