package com.candronex.order.catalog.application;

import java.util.Optional;

/** Port d'entrée du catalogue, appelé en mémoire par la prise de commande. */
public interface BrowseCatalogUseCase {

    /** @return la spécification et ses caractéristiques, ou vide si elle ne figure pas au catalogue */
    Optional<ServiceSpecificationSummary> findSpecification(String serviceSpecificationId);
}
