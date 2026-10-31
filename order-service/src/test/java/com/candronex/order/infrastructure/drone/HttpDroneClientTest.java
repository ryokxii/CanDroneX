package com.candronex.order.infrastructure.drone;

import com.candronex.order.application.DependencyUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Appel REST à Drone Service, contre un serveur HTTP local. */
class HttpDroneClientTest {

    private static final UUID OWNED = UUID.randomUUID();
    private static final UUID FOREIGN = UUID.randomUUID();
    private static final UUID BROKEN = UUID.randomUUID();
    private static final UUID SLOW = UUID.randomUUID();

    private final AtomicReference<String> receivedAuthorization = new AtomicReference<>();
    private HttpServer droneService;
    private HttpDroneClient client;

    @BeforeEach
    void start() throws IOException {
        droneService = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        droneService.createContext("/api/v1/drones/", exchange -> {
            receivedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String droneId = exchange.getRequestURI().getPath().substring("/api/v1/drones/".length());
            int status = droneId.equals(OWNED.toString()) ? 200
                    : droneId.equals(FOREIGN.toString()) ? 404
                    : droneId.equals(BROKEN.toString()) ? 500 : 200;
            if (droneId.equals(SLOW.toString())) {
                sleep(Duration.ofMillis(800));
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        droneService.start();
        client = new HttpDroneClient("http://localhost:" + droneService.getAddress().getPort(),
                Duration.ofMillis(300));
    }

    @AfterEach
    void stop() {
        droneService.stop(0);
    }

    @Test
    @DisplayName("200 : le drone appartient au client, dont le jeton est relayé")
    void ownedDroneWithRelayedToken() {
        assertThat(client.isOwnedBy(OWNED, "jeton-inspectra")).isTrue();
        assertThat(receivedAuthorization.get()).isEqualTo("Bearer jeton-inspectra");
    }

    @Test
    @DisplayName("404 : drone inexistant ou d'un autre client")
    void foreignDrone() {
        assertThat(client.isOwnedBy(FOREIGN, "jeton-aerotech")).isFalse();
    }

    @Test
    @DisplayName("500 ou délai dépassé : Drone Service est indisponible")
    void unavailableDroneService() {
        assertThatThrownBy(() -> client.isOwnedBy(BROKEN, "jeton"))
                .isInstanceOf(DependencyUnavailableException.class);
        assertThatThrownBy(() -> client.isOwnedBy(SLOW, "jeton"))
                .isInstanceOf(DependencyUnavailableException.class);
    }

    @Test
    @DisplayName("service arrêté : indisponible")
    void stoppedDroneService() {
        droneService.stop(0);

        assertThatThrownBy(() -> client.isOwnedBy(OWNED, "jeton"))
                .isInstanceOf(DependencyUnavailableException.class);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
