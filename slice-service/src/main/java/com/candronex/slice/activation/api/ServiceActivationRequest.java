package com.candronex.slice.activation.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Corps de la demande d'activation envoyée par Order Service. */
public record ServiceActivationRequest(

        @NotNull(message = "orderLineId est obligatoire")
        UUID orderLineId,

        @NotNull(message = "droneId est obligatoire")
        UUID droneId,

        @NotEmpty(message = "au moins une caractéristique est obligatoire")
        @Valid
        List<CharacteristicRequest> characteristics) {

    public record CharacteristicRequest(

            @NotBlank(message = "name est obligatoire")
            String name,

            @NotBlank(message = "value est obligatoire")
            String value) {
    }
}
