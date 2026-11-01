package com.candronex.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtDecoderConfigurationTest {

    private static final String ISSUER = "http://localhost:8081/realms/candronex";
    private static final String AUDIENCE = "candronex-api";

    private final OAuth2TokenValidator<Jwt> validator = JwtDecoderConfiguration.validator(
            new JwtProperties("http://keycloak:8080/realms/candronex/protocol/openid-connect/certs",
                    ISSUER, AUDIENCE));

    @Test
    void acceptsTokenFromTheRealmForTheApi() {
        assertThat(validator.validate(jwt(ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300)))
                .hasErrors()).isFalse();
    }

    @Test
    void rejectsTokenForAnotherAudience() {
        assertThat(validator.validate(jwt(ISSUER, List.of("account"), Instant.now().plusSeconds(300)))
                .hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenWithoutAudience() {
        assertThat(validator.validate(jwt(ISSUER, null, Instant.now().plusSeconds(300)))
                .hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        assertThat(validator.validate(jwt("http://localhost:8081/realms/autre", List.of(AUDIENCE),
                Instant.now().plusSeconds(300))).hasErrors()).isTrue();
    }

    @Test
    void rejectsExpiredToken() {
        assertThat(validator.validate(jwt(ISSUER, List.of(AUDIENCE), Instant.now().minusSeconds(120)))
                .hasErrors()).isTrue();
    }

    private static Jwt jwt(String issuer, List<String> audience, Instant expiresAt) {
        Jwt.Builder builder = Jwt.withTokenValue("jeton")
                .header("alg", "RS256")
                .issuer(issuer)
                .claim("azp", "inspectra-demo")
                .issuedAt(expiresAt.minusSeconds(300))
                .expiresAt(expiresAt);
        if (audience != null) {
            builder.audience(audience);
        }
        return builder.build();
    }
}
