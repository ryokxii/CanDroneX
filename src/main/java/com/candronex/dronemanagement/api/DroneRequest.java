package com.candronex.dronemanagement.api;

import com.candronex.dronemanagement.domain.SimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps de la requête d'enregistrement d'un drone. Le format est vérifié par le domaine. */
public record DroneRequest(

        @NotBlank(message = "imsi est obligatoire")
        String imsi,

        @NotBlank(message = "iccid est obligatoire")
        String iccid,

        @NotNull(message = "simType est obligatoire")
        SimType simType) {

    /** L'IMSI et l'ICCID ne doivent jamais apparaître dans un journal. */
    @Override
    public String toString() {
        return "DroneRequest[simType=" + simType + "]";
    }
}
