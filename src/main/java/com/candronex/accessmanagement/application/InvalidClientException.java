package com.candronex.accessmanagement.application;

/** L'authentification du client a échoué. */
public class InvalidClientException extends RuntimeException {

    public InvalidClientException() {
        super("Authentification du client échouée.");
    }
}
