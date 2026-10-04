package com.candronex.accessmanagement.published;

/** Le client qui appelle l'API n'est pas connu de la plateforme. */
public class UnknownClientException extends RuntimeException {

    public UnknownClientException(String clientId) {
        super("Client inconnu : " + clientId);
    }
}
