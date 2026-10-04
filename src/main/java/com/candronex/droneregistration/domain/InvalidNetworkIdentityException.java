package com.candronex.droneregistration.domain;

/** L'identité réseau soumise ne respecte pas les règles du domaine. */
public class InvalidNetworkIdentityException extends RuntimeException {

    public InvalidNetworkIdentityException(String message) {
        super(message);
    }
}
