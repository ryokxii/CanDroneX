package com.candronex.slice.activation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Accès Spring Data aux lignes de activation.activation_attempts. */
interface AttemptEntityStore extends JpaRepository<AttemptEntity, UUID> {

    List<AttemptEntity> findAllByServiceIdOrderByAcceptedAt(UUID serviceId);
}
