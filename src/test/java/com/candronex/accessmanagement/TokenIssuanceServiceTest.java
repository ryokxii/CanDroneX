package com.candronex.accessmanagement;

import com.candronex.accessmanagement.application.AccessToken;
import com.candronex.accessmanagement.application.ClientCredentialsGrant;
import com.candronex.accessmanagement.application.InvalidClientException;
import com.candronex.accessmanagement.application.InvalidScopeException;
import com.candronex.accessmanagement.application.TokenIssuanceService;
import com.candronex.accessmanagement.domain.ApiCredential;
import com.candronex.common.security.ApiScopes;
import com.candronex.common.security.OAuth2Properties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Émission des jetons OAuth 2.0 client_credentials, hors ligne : dépôt en mémoire, horloge figée,
 * vraie signature RSA.
 */
class TokenIssuanceServiceTest {

    private static final String CLIENT = "CLI-INSPECTRA";
    private static final String SECRET = "s3cret-de-test";
    private static final Instant NOW = Instant.parse("2026-10-04T14:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final OAuth2Properties PROPERTIES =
            new OAuth2Properties("http://candronex.test", "candronex-api", TTL);

    // Coût minimal : le test vérifie la logique, pas la résistance de BCrypt.
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private InMemoryApiCredentialRepository credentials;
    private RSAKey key;
    private TokenIssuanceService service;

    @BeforeEach
    void setUp() throws Exception {
        credentials = new InMemoryApiCredentialRepository();
        credentials.save(ApiCredential.issue(CLIENT, encoder.encode(SECRET), NOW.minusSeconds(3600), null));
        key = new RSAKeyGenerator(2048).generate();
        service = new TokenIssuanceService(credentials, encoder,
                new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key))),
                PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("émet un JWT RS256 signé dont le sujet est le client authentifié")
    void issuesSignedJwtForValidCredentials() throws Exception {
        AccessToken token = service.issue(new ClientCredentialsGrant(CLIENT, SECRET, Set.of()));

        Jwt jwt = decode(token);
        assertThat(jwt.getSubject()).isEqualTo(CLIENT);
        assertThat(jwt.getClaimAsString("client_id")).isEqualTo(CLIENT);
        assertThat(jwt.getIssuer().toString()).isEqualTo("http://candronex.test");
        assertThat(jwt.getAudience()).containsExactly("candronex-api");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256");
        assertThat(token.expiresIn()).isEqualTo(TTL);
    }

    /**
     * Vérifie la signature seulement : l'horloge du service est figée, et la validité temporelle
     * relève du serveur de ressources (testé ailleurs).
     */
    private Jwt decode(AccessToken token) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decoder.decode(token.value());
    }

    @Test
    @DisplayName("sans portée demandée, le jeton porte toutes les portées de l'API")
    void grantsAllScopesByDefault() {
        AccessToken token = service.issue(new ClientCredentialsGrant(CLIENT, SECRET, Set.of()));

        assertThat(token.scopes()).isEqualTo(ApiScopes.ALL);
    }

    @Test
    @DisplayName("une portée demandée restreint le jeton à cette portée")
    void narrowsToRequestedScopes() throws Exception {
        AccessToken token = service.issue(
                new ClientCredentialsGrant(CLIENT, SECRET, Set.of(ApiScopes.DRONES_READ)));

        assertThat(token.scopes()).containsExactly(ApiScopes.DRONES_READ);
        Jwt jwt = decode(token);
        assertThat(jwt.getClaimAsString("scope")).isEqualTo(ApiScopes.DRONES_READ);
    }

    @Test
    @DisplayName("refuse une portée qui n'existe pas")
    void rejectsUnknownScope() {
        assertThatThrownBy(() -> service.issue(
                new ClientCredentialsGrant(CLIENT, SECRET, Set.of(ApiScopes.DRONES_READ, "admin"))))
                .isInstanceOf(InvalidScopeException.class)
                .hasMessageContaining("admin");
    }

    @Test
    @DisplayName("refuse un secret erroné")
    void rejectsWrongSecret() {
        assertThatThrownBy(() -> service.issue(new ClientCredentialsGrant(CLIENT, "mauvais", Set.of())))
                .isInstanceOf(InvalidClientException.class);
    }

    @Test
    @DisplayName("refuse un secret absent")
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> service.issue(new ClientCredentialsGrant(CLIENT, null, Set.of())))
                .isInstanceOf(InvalidClientException.class);
    }

    @Test
    @DisplayName("refuse un client inconnu, avec la même erreur qu'un secret erroné")
    void rejectsUnknownClient() {
        assertThatThrownBy(() -> service.issue(new ClientCredentialsGrant("CLI-FANTOME", SECRET, Set.of())))
                .isInstanceOf(InvalidClientException.class);
        assertThatThrownBy(() -> service.issue(new ClientCredentialsGrant(null, SECRET, Set.of())))
                .isInstanceOf(InvalidClientException.class);
    }

    @Test
    @DisplayName("refuse un identifiant révoqué, même avec le bon secret")
    void rejectsRevokedCredential() {
        ApiCredential current = credentials.findByClientId(CLIENT).orElseThrow();
        credentials.save(current.revokedAt(NOW.minusSeconds(1)));

        assertThatThrownBy(() -> service.issue(new ClientCredentialsGrant(CLIENT, SECRET, Set.of())))
                .isInstanceOf(InvalidClientException.class);
    }

    @Test
    @DisplayName("refuse un identifiant expiré, même avec le bon secret")
    void rejectsExpiredCredential() {
        credentials.save(ApiCredential.issue(CLIENT, encoder.encode(SECRET),
                NOW.minusSeconds(7200), NOW.minusSeconds(1)));

        assertThatThrownBy(() -> service.issue(new ClientCredentialsGrant(CLIENT, SECRET, Set.of())))
                .isInstanceOf(InvalidClientException.class);
    }

    @Test
    @DisplayName("le secret n'apparaît pas dans la représentation textuelle de la demande")
    void grantDoesNotLeakSecret() {
        assertThat(new ClientCredentialsGrant(CLIENT, SECRET, Set.of()).toString())
                .doesNotContain(SECRET);
    }
}
