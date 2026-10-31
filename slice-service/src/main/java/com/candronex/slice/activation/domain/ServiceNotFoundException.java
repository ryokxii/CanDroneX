package com.candronex.slice.activation.domain;

import java.util.UUID;

/** Aucune activation pour cette ligne de commande. */
public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException(UUID orderLineId) {
        super("Aucune activation pour la ligne " + orderLineId + ".");
    }
}
