package com.candronex.droneregistration.api;

import com.candronex.droneregistration.domain.SimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps de la requête d'enregistrement d'un drone. */
public record DroneRequest(

        @NotBlank(message = "droneId est obligatoire")
        String droneId,

        @NotBlank(message = "imsi est obligatoire")
        String imsi,

        @NotNull(message = "simType est obligatoire")
        SimType simType) {
}
