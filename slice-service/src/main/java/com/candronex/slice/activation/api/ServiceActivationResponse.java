package com.candronex.slice.activation.api;

import com.candronex.slice.activation.application.ServiceView;

import java.time.Instant;
import java.util.UUID;

/** Représentation JSON d'un service activé. Aucune identité réseau n'y figure. */
public record ServiceActivationResponse(
        UUID serviceId,
        UUID orderLineId,
        UUID droneId,
        String state,
        Instant acceptedAt,
        Instant activatedAt) {

    static ServiceActivationResponse from(ServiceView view) {
        return new ServiceActivationResponse(
                view.serviceId(),
                view.orderLineId(),
                view.droneId(),
                view.state(),
                view.acceptedAt(),
                view.activatedAt());
    }
}
