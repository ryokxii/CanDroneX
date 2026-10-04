package com.candronex.accessmanagement.application;

import java.time.Duration;
import java.util.Set;

/** Jeton d'accès émis, prêt à être rendu par le point de terminaison OAuth 2.0. */
public record AccessToken(String value, Duration expiresIn, Set<String> scopes) {

    public AccessToken {
        scopes = Set.copyOf(scopes);
    }
}
