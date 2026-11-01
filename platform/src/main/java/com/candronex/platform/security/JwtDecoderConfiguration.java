package com.candronex.platform.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Revalide le jeton transmis par la passerelle : signature, expiration, émetteur et audience. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtDecoderConfiguration {

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        // L'émetteur public (localhost:8081) n'est pas joignable depuis un conteneur.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        decoder.setJwtValidator(validator(properties));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validator(JwtProperties properties) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                audience(properties.audience()));
    }

    private static OAuth2TokenValidator<Jwt> audience(String expected) {
        OAuth2Error error = new OAuth2Error("invalid_token", "Audience attendue : " + expected, null);
        return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(expected)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(error);
    }
}
