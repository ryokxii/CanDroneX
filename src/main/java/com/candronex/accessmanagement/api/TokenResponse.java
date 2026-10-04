package com.candronex.accessmanagement.api;

import com.candronex.accessmanagement.application.AccessToken;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.TreeSet;

/** Réponse de succès du point de terminaison de jeton. */
record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("scope") String scope) {

    static TokenResponse from(AccessToken token) {
        return new TokenResponse(
                token.value(),
                "Bearer",
                token.expiresIn().toSeconds(),
                String.join(" ", new TreeSet<>(token.scopes())));
    }
}
