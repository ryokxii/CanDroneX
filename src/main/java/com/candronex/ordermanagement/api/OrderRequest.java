package com.candronex.ordermanagement.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Corps de la requête de commande. */
public record OrderRequest(

        @NotEmpty(message = "une commande doit comporter au moins une ligne")
        @Valid
        List<OrderLineRequest> orderLines) {

    /** Une ligne : un service du catalogue, désigné par sa spécification, pour un drone. */
    public record OrderLineRequest(

            @NotNull(message = "droneId est obligatoire")
            UUID droneId,

            @NotBlank(message = "serviceSpecificationId est obligatoire")
            String serviceSpecificationId) {
    }
}
