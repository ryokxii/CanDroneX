package com.candronex.ordermanagement.api;

import com.candronex.servicecatalog.published.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Corps de la requête de commande. */
public record OrderRequest(

        @NotEmpty(message = "une commande doit comporter au moins une ligne")
        @Valid
        List<OrderLineRequest> orderLines) {

    public record OrderLineRequest(

            @NotBlank(message = "droneId est obligatoire")
            String droneId,

            @NotNull(message = "serviceType est obligatoire")
            ServiceType serviceType) {
    }
}
