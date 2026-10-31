package com.candronex.order.infrastructure.slice;

import com.candronex.order.application.DependencyUnavailableException;
import com.candronex.order.domain.ServiceCharacteristic;
import com.candronex.order.domain.port.ServiceState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Appels à Slice Service et au serveur de jetons, contre un serveur HTTP local. */
class HttpSliceServiceClientTest {

    private static final UUID KNOWN = UUID.randomUUID();
    private static final UUID BROKEN = UUID.randomUUID();
    private static final List<ServiceCharacteristic> C2 = List.of(new ServiceCharacteristic("sst", "2"));

    private final AtomicInteger tokenCalls = new AtomicInteger();
    private final AtomicReference<String> receivedAuthorization = new AtomicReference<>();
    private HttpServer server;
    private HttpSliceServiceClient client;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/token", exchange -> {
            tokenCalls.incrementAndGet();
            reply(exchange, 200, "{\"access_token\":\"jeton-order-service\",\"expires_in\":300}");
        });
        server.createContext("/internal/v1/service-activations", exchange -> {
            receivedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String query = String.valueOf(exchange.getRequestURI().getQuery());
            if (exchange.getRequestMethod().equals("POST")) {
                reply(exchange, 202, "{\"serviceId\":\"" + UUID.randomUUID() + "\",\"state\":\"PENDING\"}");
            } else if (query.contains(KNOWN.toString())) {
                reply(exchange, 200, "{\"serviceId\":\"" + UUID.randomUUID() + "\",\"state\":\"ACTIVE\"}");
            } else if (query.contains(BROKEN.toString())) {
                reply(exchange, 500, "{}");
            } else {
                reply(exchange, 404, "{}");
            }
        });
        server.start();
        String base = "http://localhost:" + server.getAddress().getPort();
        Duration timeout = Duration.ofMillis(500);
        client = new HttpSliceServiceClient(base, timeout,
                new ServiceTokenProvider(base + "/token", "order-service", "secret", timeout, Clock.systemUTC()));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    @DisplayName("demande d'activation avec le jeton de service, obtenu une seule fois")
    void requestsWithCachedServiceToken() {
        assertThat(client.request(UUID.randomUUID(), UUID.randomUUID(), C2)).isEqualTo(ServiceState.PENDING);
        assertThat(client.findByOrderLine(KNOWN)).contains(ServiceState.ACTIVE);

        assertThat(receivedAuthorization.get()).isEqualTo("Bearer jeton-order-service");
        assertThat(tokenCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("aucune activation pour la ligne : vide")
    void unknownOrderLine() {
        assertThat(client.findByOrderLine(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("500 ou service arrêté : indisponible")
    void unavailable() {
        assertThatThrownBy(() -> client.findByOrderLine(BROKEN))
                .isInstanceOf(DependencyUnavailableException.class);

        server.stop(0);
        assertThatThrownBy(() -> client.request(UUID.randomUUID(), UUID.randomUUID(), C2))
                .isInstanceOf(DependencyUnavailableException.class);
    }

    private static void reply(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
