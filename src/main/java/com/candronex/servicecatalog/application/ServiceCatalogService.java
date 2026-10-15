package com.candronex.servicecatalog.application;

import com.candronex.servicecatalog.domain.ServiceSpecification;
import com.candronex.servicecatalog.domain.ServiceSpecificationId;
import com.candronex.servicecatalog.domain.port.ServiceSpecificationRepository;
import com.candronex.servicecatalog.published.ServiceCatalog;
import com.candronex.servicecatalog.published.ServiceSpecificationSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Réalise l'interface publiée ServiceCatalog. */
@Service
class ServiceCatalogService implements ServiceCatalog {

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
                specification.version(),
                specification.characteristics().stream()
                        .map(characteristic -> new ServiceSpecificationSummary.Characteristic(
                                characteristic.name(), characteristic.defaultValue()))
                        .toList());
    }
}
