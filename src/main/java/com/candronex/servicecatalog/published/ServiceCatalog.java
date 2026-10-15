package com.candronex.servicecatalog.published;

import java.util.Optional;

/** Interface publiée par ServiceCatalog. */
public interface ServiceCatalog {

    /**
     * @return la spécification courante et ses caractéristiques, ou vide si l'identifiant ne
     *         désigne aucun service de l'offre
     */
    Optional<ServiceSpecificationSummary> findSpecification(String serviceSpecificationId);
}
