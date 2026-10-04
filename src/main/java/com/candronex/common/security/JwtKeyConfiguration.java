package com.candronex.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Clé RSA de signature des jetons, générée au démarrage et gardée en mémoire :
 * un redémarrage invalide les jetons en cours (une instance unique en Phase 1).
 */
@Configuration
@EnableConfigurationProperties(OAuth2Properties.class)
public class JwtKeyConfiguration {

    private static final int RSA_KEY_SIZE = 2048;

    @Bean
    RSAKey jwtSigningKey() throws JOSEException {
        return new RSAKeyGenerator(RSA_KEY_SIZE)
                .keyID(UUID.randomUUID().toString())
                .generate();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(jwtSigningKey)));
    }

    /**
     * Vérifie la signature (RS256 seulement : pas de « none », pas de confusion HS256), puis
     * l'émetteur, l'audience et la validité temporelle.
     */
    @Bean
    JwtDecoder jwtDecoder(RSAKey jwtSigningKey, OAuth2Properties properties, Clock clock)
            throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(jwtSigningKey.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(validator(properties, clock));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validator(OAuth2Properties properties, Clock clock) {
        JwtTimestampValidator timestamps = new JwtTimestampValidator();
        timestamps.setClock(clock);

        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(properties.audience()));
        OAuth2TokenValidator<Jwt> subject = jwt -> jwt.getSubject() == null || jwt.getSubject().isBlank()
                ? OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "sub absent", null))
                : OAuth2TokenValidatorResult.success();

        return new DelegatingOAuth2TokenValidator<>(
                timestamps,
                new JwtIssuerValidator(properties.issuer()),
                audience,
                subject);
    }
}
