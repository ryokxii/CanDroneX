package com.candronex.order.catalog.application;

import com.candronex.order.catalog.domain.ServiceSpecification;
import com.candronex.order.catalog.domain.ServiceSpecificationId;
import com.candronex.order.catalog.domain.port.ServiceSpecificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Réalise BrowseCatalogUseCase. */
@Service
class ServiceCatalogService implements BrowseCatalogUseCase {

    private final ServiceSpecificationRepository specifications;

    ServiceCatalogService(ServiceSpecificationRepository specifications) {
        this.specifications = specifications;
    }

    /** Un identifiant mal formé ne désigne aucun service : la réponse est la même qu'un inconnu. */
    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceSpecificationSummary> findSpecification(String serviceSpecificationId) {
        if (!ServiceSpecificationId.isWellFormed(serviceSpecificationId)) {
            return Optional.empty();
        }
        return specifications.findById(ServiceSpecificationId.of(serviceSpecificationId))
                .map(ServiceCatalogService::summarize);
    }

    private static ServiceSpecificationSummary summarize(ServiceSpecification specification) {
        return new ServiceSpecificationSummary(
                specification.id().value(),
                specification.characteristics().stream()
                        .map(characteristic -> new ServiceSpecificationSummary.Characteristic(
                                characteristic.name(), characteristic.defaultValue()))
                        .toList());
    }
}
