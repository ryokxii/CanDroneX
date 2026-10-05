package com.candronex.ordermanagement.api;

import com.candronex.servicecatalog.published.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Corps de la requête de commande. */
public record OrderRequest(

        @NotEmpty(message = "une commande doit comporter au moins une ligne")
        @Valid
        List<OrderLineRequest> orderLines) {

    public record OrderLineRequest(

            @NotNull(message = "droneId est obligatoire")
            UUID droneId,

            @NotNull(message = "serviceType est obligatoire")
            ServiceType serviceType) {
    }
}
