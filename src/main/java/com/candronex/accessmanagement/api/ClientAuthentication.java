package com.candronex.accessmanagement.api;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Identifiants du client (RFC 6749 §2.3.1) : en-tête Basic ou corps du formulaire, jamais les deux. */
record ClientAuthentication(String clientId, String clientSecret, boolean viaBasic) {

    private static final String BASIC_PREFIX = "Basic ";

    static ClientAuthentication resolve(String authorization, String bodyClientId,
                                        String bodyClientSecret) {
        boolean hasBasic = authorization != null
                && authorization.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length());
        boolean hasBody = bodyClientId != null || bodyClientSecret != null;

        if (hasBasic && hasBody) {
            throw new InvalidTokenRequestException("invalid_request",
                    "Un seul mode d'authentification du client est permis.");
        }
        if (hasBasic) {
            return fromBasic(authorization.substring(BASIC_PREFIX.length()).trim());
        }
        if (bodyClientId == null || bodyClientId.isBlank()) {
            throw new InvalidTokenRequestException("invalid_client",
                    "Les identifiants du client sont obligatoires.");
        }
        return new ClientAuthentication(bodyClientId, bodyClientSecret, false);
    }

    private static ClientAuthentication fromBasic(String encoded) {
        try {
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator <= 0) {
                throw malformedBasic();
            }
            // RFC 6749 §2.3.1 : chaque partie est form-urlencoded avant le Base64.
            String clientId = URLDecoder.decode(decoded.substring(0, separator), StandardCharsets.UTF_8);
            String secret = URLDecoder.decode(decoded.substring(separator + 1), StandardCharsets.UTF_8);
            return new ClientAuthentication(clientId, secret, true);
        } catch (IllegalArgumentException malformed) {
            // Base64 invalide ou séquence %xx incorrecte.
            throw malformedBasic();
        }
    }

    private static InvalidTokenRequestException malformedBasic() {
        return new InvalidTokenRequestException("invalid_request",
                "En-tête Authorization Basic mal formé.");
    }

    @Override
    public String toString() {
        return "ClientAuthentication[clientId=" + clientId + ", viaBasic=" + viaBasic + "]";
    }
}
