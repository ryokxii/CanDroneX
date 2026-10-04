package com.candronex.droneregistration.api;

import com.candronex.droneregistration.application.DroneView;
import com.candronex.droneregistration.application.RegisterDroneCommand;
import com.candronex.droneregistration.application.RegisterDroneService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Adaptateur entrant du module DroneRegistration. */
@RestController
@RequestMapping("/api/v1/drones")
class DroneController {


    private final RegisterDroneService drones;

    DroneController(RegisterDroneService drones) {
        this.drones = drones;
    }

    @PostMapping
    ResponseEntity<DroneResponse> register(
            @AuthenticationPrincipal(expression = "subject") String clientId,
            @Valid @RequestBody DroneRequest request) {

        DroneView registered = drones.register(new RegisterDroneCommand(
                clientId, request.droneId(), request.imsi(), request.simType()));

        return ResponseEntity
                .created(URI.create("/api/v1/drones/" + registered.droneId()))
                .body(DroneResponse.from(registered));
    }

    @GetMapping
    List<DroneResponse> listMine(@AuthenticationPrincipal(expression = "subject") String clientId) {
        return drones.listForClient(clientId).stream()
                .map(DroneResponse::from)
                .toList();
    }

    @GetMapping("/{droneId}")
    DroneResponse findMine(
            @AuthenticationPrincipal(expression = "subject") String clientId,
            @PathVariable String droneId) {

        return DroneResponse.from(drones.findForClient(droneId, clientId));
    }
}
