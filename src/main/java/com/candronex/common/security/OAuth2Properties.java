package com.candronex.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/** Paramètres des jetons d'accès, partagés par l'émetteur et le vérificateur. */
@ConfigurationProperties(prefix = "candronex.security.oauth2")
public record OAuth2Properties(String issuer, String audience, Duration accessTokenTtl) {

    public OAuth2Properties {
        Objects.requireNonNull(issuer, "candronex.security.oauth2.issuer");
        Objects.requireNonNull(audience, "candronex.security.oauth2.audience");
        Objects.requireNonNull(accessTokenTtl, "candronex.security.oauth2.access-token-ttl");
        if (accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("access-token-ttl doit être strictement positif.");
        }
    }
}
