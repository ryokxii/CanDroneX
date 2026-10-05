package com.candronex.accessmanagement.published;

import java.util.UUID;

/** Le client qui appelle l'API n'est pas connu de la plateforme. */
public class UnknownClientException extends RuntimeException {

    public UnknownClientException(UUID clientId) {
        super("Client inconnu : " + clientId);
    }
}
