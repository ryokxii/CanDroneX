package com.candronex.ordermanagement.domain;

import com.candronex.servicecatalog.published.ServiceType;

/** Deux lignes de la même commande visent le même couple drone + type de service. */
public class DuplicateOrderLineException extends RuntimeException {

    public DuplicateOrderLineException(String droneId, ServiceType serviceType) {
        super("Le service " + serviceType + " est déjà demandé pour le drone "
                + droneId + " dans cette commande.");
    }
}
