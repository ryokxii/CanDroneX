package com.candronex.accessmanagement.application;

import com.candronex.accessmanagement.domain.ApiCredential;
import com.candronex.accessmanagement.domain.port.ApiCredentialRepository;
import com.candronex.common.security.ApiScopes;
import com.candronex.common.security.OAuth2Properties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Émet les jetons d'accès JWT du flux OAuth 2.0 client_credentials. */
@Service
public class TokenIssuanceService {

    private final ApiCredentialRepository credentials;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final OAuth2Properties properties;
    private final Clock clock;

    /**
     * Empreinte comparée quand le client est inconnu, pour que la réponse prenne le même temps
     * qu'un secret erroné : le délai ne doit pas révéler quels clientId existent.
     */
    private final String decoyHash;

    public TokenIssuanceService(ApiCredentialRepository credentials,
                                PasswordEncoder passwordEncoder,
                                JwtEncoder jwtEncoder,
                                OAuth2Properties properties,
                                Clock clock) {
        this.credentials = credentials;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
        this.decoyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * @throws InvalidClientException si le client ne s'authentifie pas
     * @throws InvalidScopeException  si une portée demandée n'est pas permise
     */
    @Transactional(readOnly = true)
    public AccessToken issue(ClientCredentialsGrant grant) {
        Instant now = clock.instant();
        ApiCredential credential = authenticate(grant, now);
        Set<String> scopes = grantedScopes(grant.requestedScopes());

        Instant expiresAt = now.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(credential.clientId())
                .audience(List.of(properties.audience()))
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("client_id", credential.clientId())
                .claim("scope", String.join(" ", new TreeSet<>(scopes)))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, properties.accessTokenTtl(), scopes);
    }

    private ApiCredential authenticate(ClientCredentialsGrant grant, Instant now) {
        String secret = grant.clientSecret() == null ? "" : grant.clientSecret();
        ApiCredential credential = grant.clientId() == null
                ? null
                : credentials.findByClientId(grant.clientId()).orElse(null);

        String hash = credential == null ? decoyHash : credential.secretHash();
        boolean secretMatches = passwordEncoder.matches(secret, hash);

        if (credential == null || !secretMatches || !credential.isUsableAt(now)) {
            throw new InvalidClientException();
        }
        return credential;
    }

    private static Set<String> grantedScopes(Set<String> requested) {
        if (requested.isEmpty()) {
            return ApiScopes.ALL;
        }
        Set<String> rejected = new HashSet<>(requested);
        rejected.removeAll(ApiScopes.ALL);
        if (!rejected.isEmpty()) {
            throw new InvalidScopeException(rejected);
        }
        return Set.copyOf(requested);
    }
}
