package com.candronex.common.security;

import com.candronex.accessmanagement.InMemoryApiCredentialRepository;
import com.candronex.accessmanagement.application.TokenIssuanceService;
import com.candronex.accessmanagement.domain.ApiCredential;
import com.candronex.accessmanagement.domain.port.ApiCredentialRepository;
import com.candronex.common.TimeConfiguration;
import com.candronex.droneregistration.application.DroneView;
import com.candronex.droneregistration.application.RegisterDroneCommand;
import com.candronex.droneregistration.application.RegisterDroneService;
import com.candronex.ordermanagement.application.OrderView;
import com.candronex.ordermanagement.application.PlaceOrderCommand;
import com.candronex.ordermanagement.application.PlaceOrderResult;
import com.candronex.ordermanagement.application.PlaceOrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le flux OAuth 2.0 de bout en bout, à travers la vraie chaîne de sécurité : obtenir un jeton sur
 * /oauth2/token, puis appeler l'API avec ce jeton.
 */
@WebMvcTest
@Import({SecurityConfiguration.class, JwtKeyConfiguration.class, TimeConfiguration.class,
        TokenIssuanceService.class, OAuth2FlowTest.Credentials.class})
class OAuth2FlowTest {

    static final UUID CLIENT_ID = UUID.randomUUID();
    static final String CLIENT = CLIENT_ID.toString();
    static final String SECRET = "inspectra-demo-secret";
    static final UUID REVOKED_CLIENT_ID = UUID.randomUUID();
    static final UUID DRONE_ID = UUID.randomUUID();

    @TestConfiguration
    static class Credentials {

        @Bean
        ApiCredentialRepository apiCredentialRepository() {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
            Instant issued = Instant.now().minusSeconds(3600);
            InMemoryApiCredentialRepository repository = new InMemoryApiCredentialRepository();
            repository.save(ApiCredential.issue(CLIENT_ID, encoder.encode(SECRET), issued, null));
            repository.save(ApiCredential.issue(REVOKED_CLIENT_ID, encoder.encode(SECRET), issued, null)
                    .revokedAt(issued.plusSeconds(1)));
            return repository;
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    OAuth2Properties properties;

    @MockitoBean
    RegisterDroneService drones;

    @MockitoBean
    PlaceOrderService orders;

    /** @MockitoBean n'est pas réinitialisé entre les tests des classes @Nested. */
    @BeforeEach
    void resetMocks() {
        reset(drones, orders);
    }

    static String basic(String id, String secret) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((id + ":" + secret).getBytes(StandardCharsets.UTF_8));
    }

    static MockHttpServletRequestBuilder tokenRequest() {
        return post("/oauth2/token").contentType(MediaType.APPLICATION_FORM_URLENCODED);
    }

    String obtainToken(String... scope) throws Exception {
        MockHttpServletRequestBuilder request = tokenRequest()
                .header(HttpHeaders.AUTHORIZATION, basic(CLIENT, SECRET))
                .param("grant_type", "client_credentials");
        if (scope.length > 0) {
            request.param("scope", String.join(" ", scope));
        }
        String body = mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("access_token").asText();
    }

    /** Forge un jeton signé par la vraie clé, avec des revendications altérées. */
    String forgeToken(Consumer<JwtClaimsSet.Builder> customizer) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(CLIENT)
                .audience(List.of(properties.audience()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("scope", String.join(" ", ApiScopes.ALL));
        customizer.accept(claims);
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build())).getTokenValue();
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }

    static DroneView droneOf(UUID clientId) {
        return new DroneView(DRONE_ID, clientId, "302720123456789", "ESIM", "REGISTERED",
                Instant.parse("2026-10-04T14:00:00Z"));
    }

    @Nested
    @DisplayName("POST /oauth2/token")
    class TokenEndpoint {

        @Test
        @DisplayName("émet un jeton Bearer pour client_secret_basic")
        void issuesTokenWithBasicAuthentication() throws Exception {
            String body = mvc.perform(tokenRequest()
                            .header(HttpHeaders.AUTHORIZATION, basic(CLIENT, SECRET))
                            .param("grant_type", "client_credentials"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                    .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.expires_in").value(properties.accessTokenTtl().toSeconds()))
                    .andExpect(jsonPath("$.scope")
                            .value("drones:read drones:write orders:read orders:write"))
                    .andReturn().getResponse().getContentAsString();

            JsonNode token = json.readTree(body);
            assertThat(token.get("access_token").asText().split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("émet un jeton pour client_secret_post, restreint à la portée demandée")
        void issuesTokenWithPostAuthentication() throws Exception {
            mvc.perform(tokenRequest()
                            .param("grant_type", "client_credentials")
                            .param("client_id", CLIENT)
                            .param("client_secret", SECRET)
                            .param("scope", "drones:read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scope").value("drones:read"));
        }

        @Test
        @DisplayName("401 invalid_client avec défi Basic pour un secret erroné")
        void rejectsWrongSecretViaBasic() throws Exception {
            mvc.perform(tokenRequest()
                            .header(HttpHeaders.AUTHORIZATION, basic(CLIENT, "mauvais"))
                            .param("grant_type", "client_credentials"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Basic")))
                    .andExpect(jsonPath("$.error").value("invalid_client"))
                    .andExpect(jsonPath("$.access_token").doesNotExist());
        }

        @Test
        @DisplayName("401 invalid_client pour un client inconnu ou révoqué, sans distinguer la cause")
        void rejectsUnknownAndRevokedClientsIdentically() throws Exception {
            for (String clientId : new String[] {
                    UUID.randomUUID().toString(), "pas-un-uuid", REVOKED_CLIENT_ID.toString()}) {
                mvc.perform(tokenRequest()
                                .param("grant_type", "client_credentials")
                                .param("client_id", clientId)
                                .param("client_secret", SECRET))
                        .andExpect(status().isUnauthorized())
                        .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                        .andExpect(jsonPath("$.error").value("invalid_client"))
                        .andExpect(jsonPath("$.error_description").value("Authentification du client échouée."));
            }
        }

        @Test
        @DisplayName("401 invalid_client sans aucun identifiant")
        void rejectsMissingClientCredentials() throws Exception {
            mvc.perform(tokenRequest().param("grant_type", "client_credentials"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("invalid_client"));
        }

        @Test
        @DisplayName("400 invalid_request sans grant_type")
        void rejectsMissingGrantType() throws Exception {
            mvc.perform(tokenRequest().header(HttpHeaders.AUTHORIZATION, basic(CLIENT, SECRET)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_request"));
        }

        @Test
        @DisplayName("400 unsupported_grant_type pour un autre flux que client_credentials")
        void rejectsOtherGrantTypes() throws Exception {
            mvc.perform(tokenRequest()
                            .header(HttpHeaders.AUTHORIZATION, basic(CLIENT, SECRET))
                            .param("grant_type", "password"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
        }

        @Test
        @DisplayName("400 invalid_scope pour une portée hors de l'API")
        void rejectsUnknownScope() throws Exception {
            mvc.perform(tokenRequest()
                            .header(HttpHeaders.AUTHORIZATION, basic(CLIENT, SECRET))
                            .param("grant_type", "client_credentials")
                            .param("scope", "drones:read admin"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_scope"));
        }

        @Test
        @DisplayName("400 invalid_request si le secret est passé dans l'URL")
        void rejectsSecretInQueryString() throws Exception {
            mvc.perform(post("/oauth2/token?client_id=" + CLIENT + "&client_secret=" + SECRET)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("grant_type", "client_credentials"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_request"));
        }

        @Test
        @DisplayName("415 pour un corps JSON au lieu d'un formulaire")
        void rejectsJsonBody() throws Exception {
            mvc.perform(post("/oauth2/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grant_type\":\"client_credentials\"}"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    @Nested
    @DisplayName("Appels authentifiés à l'API")
    class ResourceServer {

        @Test
        @DisplayName("un jeton obtenu sur /oauth2/token donne accès aux drones du client du jeton")
        void tokenGrantsAccessAsTokenSubject() throws Exception {
            when(drones.listForClient(CLIENT_ID)).thenReturn(List.of(droneOf(CLIENT_ID)));
            String token = obtainToken();

            mvc.perform(get("/api/v1/drones").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].droneId").value(DRONE_ID.toString()));

            verify(drones).listForClient(CLIENT_ID);
        }

        @Test
        @DisplayName("l'en-tête X-Client-Id n'authentifie plus, et ne peut pas usurper un autre client")
        void clientHeaderIsNoLongerTrusted() throws Exception {
            mvc.perform(get("/api/v1/drones").header("X-Client-Id", CLIENT))
                    .andExpect(status().isUnauthorized());

            UUID victim = UUID.randomUUID();
            when(drones.listForClient(any())).thenReturn(List.of());
            mvc.perform(get("/api/v1/drones")
                            .header(HttpHeaders.AUTHORIZATION, bearer(obtainToken()))
                            .header("X-Client-Id", victim.toString()))
                    .andExpect(status().isOk());

            verify(drones).listForClient(CLIENT_ID);
            verify(drones, never()).listForClient(victim);
        }

        @Test
        @DisplayName("la commande est passée au nom du sujet du jeton")
        void orderIsPlacedForTokenSubject() throws Exception {
            OrderView view = new OrderView(UUID.randomUUID(), CLIENT_ID, "RECEIVED",
                    Instant.parse("2026-10-04T14:00:00Z"), null, List.of());
            when(orders.placeOrder(any())).thenReturn(new PlaceOrderResult(view, true));

            mvc.perform(post("/api/v1/service-orders")
                            .header(HttpHeaders.AUTHORIZATION, bearer(obtainToken(ApiScopes.ORDERS_WRITE)))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"orderLines":[{"droneId":"%s","serviceType":"IMAGERY_EMBB"}]}
                                    """.formatted(DRONE_ID)))
                    .andExpect(status().isCreated());

            ArgumentCaptor<PlaceOrderCommand> command = ArgumentCaptor.forClass(PlaceOrderCommand.class);
            verify(orders).placeOrder(command.capture());
            assertThat(command.getValue().clientId()).isEqualTo(CLIENT_ID);
        }

        @Test
        @DisplayName("401 avec défi Bearer et Problem Details sans jeton")
        void rejectsMissingToken() throws Exception {
            mvc.perform(get("/api/v1/drones"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/unauthenticated"))
                    .andExpect(jsonPath("$.correlationId").exists());
        }

        @Test
        @DisplayName("401 invalid_token pour un jeton illisible")
        void rejectsMalformedToken() throws Exception {
            mvc.perform(get("/api/v1/drones").header(HttpHeaders.AUTHORIZATION, bearer("pas.un.jwt")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
        }

        @Test
        @DisplayName("401 pour un jeton signé par une autre clé")
        void rejectsTokenSignedByAnotherKey() throws Exception {
            JwtEncoder attacker = new NimbusJwtEncoder(
                    new ImmutableJWKSet<>(new JWKSet(new RSAKeyGenerator(2048).generate())));
            Instant now = Instant.now();
            String token = attacker.encode(JwtEncoderParameters.from(
                    JwsHeader.with(SignatureAlgorithm.RS256).build(),
                    JwtClaimsSet.builder()
                            .issuer(properties.issuer())
                            .subject(CLIENT)
                            .audience(List.of(properties.audience()))
                            .issuedAt(now)
                            .expiresAt(now.plusSeconds(300))
                            .claim("scope", String.join(" ", ApiScopes.ALL))
                            .build())).getTokenValue();

            mvc.perform(get("/api/v1/drones").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 pour un jeton non signé (alg: none)")
        void rejectsUnsignedToken() throws Exception {
            Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
            long exp = Instant.now().plusSeconds(300).getEpochSecond();
            String header = b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
            String payload = b64.encodeToString(("{\"iss\":\"" + properties.issuer() + "\",\"sub\":\"" + CLIENT
                    + "\",\"aud\":\"" + properties.audience() + "\",\"exp\":" + exp
                    + ",\"scope\":\"drones:read\"}").getBytes(StandardCharsets.UTF_8));

            mvc.perform(get("/api/v1/drones")
                            .header(HttpHeaders.AUTHORIZATION, bearer(header + "." + payload + ".")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 pour un jeton expiré")
        void rejectsExpiredToken() throws Exception {
            Instant past = Instant.now().minusSeconds(3600);
            String token = forgeToken(claims -> claims.issuedAt(past).expiresAt(past.plusSeconds(60)));

            mvc.perform(get("/api/v1/drones").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 pour un jeton destiné à une autre audience")
        void rejectsWrongAudience() throws Exception {
            String token = forgeToken(claims -> claims.audience(List.of("autre-api")));

            mvc.perform(get("/api/v1/drones").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 pour un jeton d'un autre émetteur")
        void rejectsWrongIssuer() throws Exception {
            String token = forgeToken(claims -> claims.issuer("https://emetteur.malveillant"));

            mvc.perform(get("/api/v1/drones").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 insufficient_scope : un jeton de lecture ne permet pas d'écrire")
        void rejectsInsufficientScope() throws Exception {
            String readOnly = obtainToken(ApiScopes.DRONES_READ);

            mvc.perform(post("/api/v1/drones")
                            .header(HttpHeaders.AUTHORIZATION, bearer(readOnly))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"imsi":"302720123456789","simType":"ESIM"}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("insufficient_scope")))
                    .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/insufficient-scope"));

            mvc.perform(get("/api/v1/service-orders/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(readOnly)))
                    .andExpect(status().isForbidden());

            verify(drones, never()).register(any(RegisterDroneCommand.class));
        }

        @Test
        @DisplayName("400 pour un identifiant de ressource qui n'est pas un UUID")
        void rejectsMalformedResourceId() throws Exception {
            mvc.perform(get("/api/v1/drones/DRN-0001")
                            .header(HttpHeaders.AUTHORIZATION, bearer(obtainToken())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/malformed-parameter"));

            verify(drones, never()).findForClient(any(), any());
        }

        @Test
        @DisplayName("une route non déclarée est refusée par défaut")
        void deniesUndeclaredRoutes() throws Exception {
            mvc.perform(get("/api/v1/admin")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1/admin").header(HttpHeaders.AUTHORIZATION, bearer(obtainToken())))
                    .andExpect(status().isForbidden());
        }
    }
}
