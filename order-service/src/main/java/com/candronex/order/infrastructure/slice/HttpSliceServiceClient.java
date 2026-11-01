package com.candronex.order.infrastructure.slice;

import com.candronex.order.application.DependencyUnavailableException;
import com.candronex.order.domain.ServiceCharacteristic;
import com.candronex.order.domain.port.ServiceActivationPort;
import com.candronex.order.domain.port.ServiceState;
import com.candronex.platform.logging.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : /internal/v1/service-activations sur Slice Service, avec le jeton de service. */
@Component
class HttpSliceServiceClient implements ServiceActivationPort {

    private static final String ACTIVATIONS = "/internal/v1/service-activations";

    private final RestClient slices;
    private final ServiceTokenProvider tokens;

    HttpSliceServiceClient(@Value("${candronex.slice-service.url}") String baseUrl,
                           @Value("${candronex.slice-service.timeout}") Duration timeout,
                           ServiceTokenProvider tokens) {
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(timeout);
        requests.setReadTimeout(timeout);
        this.slices = RestClient.builder().baseUrl(baseUrl).requestFactory(requests).build();
        this.tokens = tokens;
    }

    @Override
    public ServiceState request(UUID orderLineId, UUID droneId, List<ServiceCharacteristic> characteristics) {
        try {
            return slices.post()
                    .uri(ACTIVATIONS)
                    .headers(this::authenticate)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ActivationRequest(orderLineId, droneId, characteristics.stream()
                            .map(characteristic -> new Characteristic(characteristic.name(), characteristic.value()))
                            .toList()))
                    .retrieve()
                    .body(Activation.class)
                    .state();
        } catch (RestClientException unavailable) {
            throw new DependencyUnavailableException("Slice Service indisponible", unavailable);
        }
    }

    @Override
    public Optional<ServiceState> findByOrderLine(UUID orderLineId) {
        try {
            return Optional.of(slices.get()
                    .uri(ACTIVATIONS + "?orderLineId={orderLineId}", orderLineId)
                    .headers(this::authenticate)
                    .retrieve()
                    .body(Activation.class)
                    .state());
        } catch (HttpClientErrorException.NotFound none) {
            return Optional.empty();
        } catch (RestClientException unavailable) {
            throw new DependencyUnavailableException("Slice Service indisponible", unavailable);
        }
    }

    private void authenticate(HttpHeaders headers) {
        headers.setBearerAuth(tokens.token());
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            headers.set(CorrelationIdFilter.HEADER, correlationId);
        }
    }

    record ActivationRequest(UUID orderLineId, UUID droneId, List<Characteristic> characteristics) {
    }

    record Characteristic(String name, String value) {
    }

    record Activation(UUID serviceId, ServiceState state) {
    }
}
