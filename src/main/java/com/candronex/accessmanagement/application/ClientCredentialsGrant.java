package com.candronex.accessmanagement.application;

import java.util.Set;

/** Demande de jeton par le flux client_credentials. */
public record ClientCredentialsGrant(String clientId, String clientSecret,
                                     Set<String> requestedScopes) {

    public ClientCredentialsGrant {
        requestedScopes = requestedScopes == null ? Set.of() : Set.copyOf(requestedScopes);
    }

    /** Le secret ne doit jamais apparaître dans un journal. */
    @Override
    public String toString() {
        return "ClientCredentialsGrant[clientId=" + clientId + ", requestedScopes=" + requestedScopes + "]";
    }
}
