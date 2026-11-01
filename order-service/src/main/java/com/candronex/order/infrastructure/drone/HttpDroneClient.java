package com.candronex.order.infrastructure.drone;

import com.candronex.order.application.DependencyUnavailableException;
import com.candronex.order.domain.port.DroneOwnershipPort;
import com.candronex.platform.logging.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.UUID;

/**
 * Adaptateur sortant : GET /api/v1/drones/{droneId} sur Drone Service, avec le jeton du client.
 * Drone Service applique lui-même sa règle de propriété : un drone d'un autre client donne 404.
 */
@Component
class HttpDroneClient implements DroneOwnershipPort {

    private static final Logger log = LoggerFactory.getLogger(HttpDroneClient.class);

    private final RestClient drones;

    HttpDroneClient(@Value("${candronex.drone-service.url}") String baseUrl,
                    @Value("${candronex.drone-service.timeout}") Duration timeout) {
        // La fabrique JDK de Spring 6.2.1 lève CancellationException au délai, hors RestClientException.
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(timeout);
        requests.setReadTimeout(timeout);
        this.drones = RestClient.builder().baseUrl(baseUrl).requestFactory(requests).build();
    }

    @Override
    public boolean isOwnedBy(UUID droneId, String clientToken) {
        long start = System.nanoTime();
        try {
            drones.get()
                    .uri("/api/v1/drones/{droneId}", droneId)
                    .headers(headers -> {
                        headers.setBearerAuth(clientToken);
                        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
                        if (correlationId != null) {
                            headers.set(CorrelationIdFilter.HEADER, correlationId);
                        }
                    })
                    .retrieve()
                    .toBodilessEntity();
            log.info("event=drone-checked droneId={} status=200 durationMs={}", droneId, elapsedMs(start));
            return true;
        } catch (HttpClientErrorException.NotFound notOwned) {
            log.info("event=drone-checked droneId={} status=404 durationMs={}", droneId, elapsedMs(start));
            return false;
        } catch (RestClientException unavailable) {
            // Délai dépassé, service arrêté ou réponse inattendue : le client réessaie plus tard.
            log.warn("event=drone-checked droneId={} durationMs={} error=dependency-unavailable",
                    droneId, elapsedMs(start));
            throw new DependencyUnavailableException("Drone Service indisponible", unavailable);
        }
    }

    private static long elapsedMs(long start) {
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }
}
