package com.candronex.order.catalog.infrastructure;

import com.candronex.order.catalog.domain.ServiceSpecification;
import com.candronex.order.catalog.domain.ServiceSpecificationId;
import com.candronex.order.catalog.domain.port.ServiceSpecificationRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptateur sortant : réalise le port ServiceSpecificationRepository sur le schéma catalog. */
@Repository
class JpaServiceSpecificationRepository implements ServiceSpecificationRepository {

    private final ServiceSpecificationEntityStore specifications;

    JpaServiceSpecificationRepository(ServiceSpecificationEntityStore specifications) {
        this.specifications = specifications;
    }

    @Override
    public Optional<ServiceSpecification> findById(ServiceSpecificationId id) {
        return specifications.findById(id.value()).map(ServiceSpecificationEntity::toDomain);
    }
}
