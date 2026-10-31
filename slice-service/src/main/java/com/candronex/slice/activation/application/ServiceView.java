package com.candronex.slice.activation.application;

import com.candronex.slice.activation.domain.Service;

import java.time.Instant;
import java.util.UUID;

/** Projection d'un service destinée à sortir du service applicatif. */
public record ServiceView(
        UUID serviceId,
        UUID orderLineId,
        UUID droneId,
        String state,
        Instant acceptedAt,
        Instant activatedAt) {

    public static ServiceView from(Service service) {
        return new ServiceView(
                service.serviceId(),
                service.orderLineId(),
                service.droneId(),
                service.state().name(),
                service.acceptedAt(),
                service.activatedAt());
    }
}
