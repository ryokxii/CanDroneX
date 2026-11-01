package com.candronex.slice.activation.application;

import com.candronex.slice.activation.domain.Service;
import com.candronex.slice.activation.domain.ServiceNotFoundException;
import com.candronex.slice.activation.domain.ServiceState;
import com.candronex.slice.activation.domain.port.NetworkProvisioningPort;
import com.candronex.slice.activation.domain.port.ProvisioningRequest;
import com.candronex.slice.activation.domain.port.ProvisioningResult;
import com.candronex.slice.activation.domain.port.ServiceActivationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Réalise ActivateServiceUseCase et FindActivationUseCase. Les appels au réseau se font hors
 * transaction ; leur résultat n'est écrit que si le service est encore dans l'état lu.
 */
@org.springframework.stereotype.Service
public class ActivationService implements ActivateServiceUseCase, FindActivationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ActivationService.class);

    private final ServiceActivationRepository services;
    private final ActivationTransitions transitions;
    private final NetworkProvisioningPort network;
    private final Clock clock;

    public ActivationService(ServiceActivationRepository services, ActivationTransitions transitions,
                             NetworkProvisioningPort network, Clock clock) {
        this.services = services;
        this.transitions = transitions;
        this.network = network;
        this.clock = clock;
    }

    @Override
    public RequestActivationResult request(RequestActivationCommand command) {
        Optional<Service> existing = services.findByOrderLineId(command.orderLineId());
        if (existing.isPresent()) {
            log.info("event=activation-replayed orderLineId={} serviceId={}",
                    command.orderLineId(), existing.get().serviceId());
            return new RequestActivationResult(ServiceView.from(existing.get()), false);
        }

        Service service = Service.request(
                command.orderLineId(), command.droneId(), command.characteristics(), clock.instant());
        try {
            transitions.add(service);
        } catch (DataIntegrityViolationException concurrentReplay) {
            // Deux demandes simultanées pour la même ligne : UNIQUE (order_line_id) départage.
            return services.findByOrderLineId(command.orderLineId())
                    .map(other -> new RequestActivationResult(ServiceView.from(other), false))
                    .orElseThrow(() -> concurrentReplay);
        }
        log.info("event=service-created orderLineId={} serviceId={} state=PENDING",
                command.orderLineId(), service.serviceId());
        return new RequestActivationResult(ServiceView.from(service), true);
    }

    @Override
    public void attempt(UUID serviceId) {
        Optional<Service> found = services.findById(serviceId);
        // Un service déjà ACTIVE n'est jamais reconfiguré.
        if (found.isEmpty() || found.get().state() != ServiceState.PENDING) {
            return;
        }
        Service service = found.get();

        long start = System.nanoTime();
        ProvisioningResult result = network.provision(
                new ProvisioningRequest(service.serviceId(), service.characteristics()));
        log.info("event=network-call port=NetworkProvisioningPort operation=provision serviceId={} durationMs={}",
                serviceId, Duration.ofNanos(System.nanoTime() - start).toMillis());

        service.complete(result, clock.instant());
        if (transitions.save(service, ServiceState.PENDING)) {
            log.info("event=activation-result serviceId={} orderLineId={} result={} state={}",
                    serviceId, service.orderLineId(), result, service.state());
        }
    }

    @Override
    public void reconcile(UUID serviceId) {
        Optional<Service> found = services.findById(serviceId);
        if (found.isEmpty() || found.get().state() != ServiceState.UNCONFIRMED) {
            return;
        }
        Service service = found.get();

        ProvisioningResult actual = network.findStatus(serviceId);
        service.reconcile(actual, clock.instant());
        if (service.state() != ServiceState.UNCONFIRMED && transitions.save(service, ServiceState.UNCONFIRMED)) {
            log.info("event=activation-reconciled serviceId={} status={} state={}",
                    serviceId, actual, service.state());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> unconfirmed() {
        return services.findAllByState(ServiceState.UNCONFIRMED).stream()
                .map(Service::serviceId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceView findByOrderLine(UUID orderLineId) {
        return services.findByOrderLineId(orderLineId)
                .map(ServiceView::from)
                .orElseThrow(() -> new ServiceNotFoundException(orderLineId));
    }
}
