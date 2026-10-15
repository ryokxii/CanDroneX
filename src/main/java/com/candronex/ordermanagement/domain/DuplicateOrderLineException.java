package com.candronex.ordermanagement.domain;

import java.util.UUID;

/** Deux lignes de la même commande visent le même couple drone + spécification de service. */
public class DuplicateOrderLineException extends RuntimeException {

    public DuplicateOrderLineException(UUID droneId, String serviceSpecificationId) {
        super("Le service " + serviceSpecificationId + " est déjà demandé pour le drone "
                + droneId + " dans cette commande.");
    }
}
