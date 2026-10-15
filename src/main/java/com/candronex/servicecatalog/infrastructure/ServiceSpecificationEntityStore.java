package com.candronex.servicecatalog.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

/** Accès Spring Data aux lignes de catalog.service_specifications. */
interface ServiceSpecificationEntityStore extends JpaRepository<ServiceSpecificationEntity, String> {
}
