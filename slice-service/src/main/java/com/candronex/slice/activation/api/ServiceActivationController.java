package com.candronex.slice.activation.api;

import com.candronex.slice.activation.application.ActivateServiceUseCase;
import com.candronex.slice.activation.application.FindActivationUseCase;
import com.candronex.slice.activation.application.RequestActivationCommand;
import com.candronex.slice.activation.application.RequestActivationResult;
import com.candronex.slice.activation.domain.ServiceCharacteristic;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** Adaptateur entrant de Slice Service ; routes internes, réservées à Order Service. */
@RestController
@RequestMapping("/internal/v1/service-activations")
class ServiceActivationController {

    private final ActivateServiceUseCase activation;
    private final FindActivationUseCase activations;

    ServiceActivationController(ActivateServiceUseCase activation, FindActivationUseCase activations) {
        this.activation = activation;
        this.activations = activations;
    }

    @PostMapping
    ResponseEntity<ServiceActivationResponse> request(@Valid @RequestBody ServiceActivationRequest request) {
        RequestActivationResult result = activation.request(new RequestActivationCommand(
                request.orderLineId(),
                request.droneId(),
                request.characteristics().stream()
                        .map(characteristic -> new ServiceCharacteristic(
                                characteristic.name(), characteristic.value()))
                        .toList()));

        ServiceActivationResponse body = ServiceActivationResponse.from(result.service());
        if (!result.created()) {
            // Rejeu : la ligne a déjà son service, rien n'a été créé.
            return ResponseEntity.ok(body);
        }
        return ResponseEntity
                .accepted()
                .location(URI.create("/internal/v1/service-activations?orderLineId=" + body.orderLineId()))
                .body(body);
    }

    @GetMapping
    ServiceActivationResponse findByOrderLine(@RequestParam UUID orderLineId) {
        return ServiceActivationResponse.from(activations.findByOrderLine(orderLineId));
    }
}
