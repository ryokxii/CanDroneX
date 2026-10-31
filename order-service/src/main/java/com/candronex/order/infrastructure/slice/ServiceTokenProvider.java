package com.candronex.order.infrastructure.slice;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Jeton de service d'Order Service (client_credentials, compte order-service), gardé en cache
 * jusqu'à peu avant son expiration. Le secret vient de l'environnement et n'est jamais journalisé.
 */
@Component
class ServiceTokenProvider {

    private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofSeconds(30);

    private final RestClient keycloak;
    private final String clientId;
    private final String clientSecret;
    private final Clock clock;
    private String token;
    private Instant renewAt = Instant.MIN;

    ServiceTokenProvider(@Value("${candronex.service-token.url}") String tokenUrl,
                         @Value("${candronex.service-token.client-id}") String clientId,
                         @Value("${candronex.service-token.client-secret}") String clientSecret,
                         @Value("${candronex.slice-service.timeout}") Duration timeout,
                         Clock clock) {
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(timeout);
        requests.setReadTimeout(timeout);
        this.keycloak = RestClient.builder().baseUrl(tokenUrl).requestFactory(requests).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clock = clock;
    }

    synchronized String token() {
        if (clock.instant().isBefore(renewAt)) {
            return token;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        TokenResponse response = keycloak.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        this.token = response.accessToken();
        this.renewAt = clock.instant().plusSeconds(response.expiresIn()).minus(RENEW_BEFORE_EXPIRY);
        return token;
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") long expiresIn) {
    }
}
