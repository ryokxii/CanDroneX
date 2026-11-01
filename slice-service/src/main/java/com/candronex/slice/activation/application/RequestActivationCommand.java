package com.candronex.slice.activation.application;

import com.candronex.slice.activation.domain.ServiceCharacteristic;

import java.util.List;
import java.util.UUID;

/** Demande d'activation du service d'une ligne de commande. */
public record RequestActivationCommand(
        UUID orderLineId,
        UUID droneId,
        List<ServiceCharacteristic> characteristics) {
}
