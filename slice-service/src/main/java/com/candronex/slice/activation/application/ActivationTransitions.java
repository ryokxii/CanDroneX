package com.candronex.slice.activation.application;

import com.candronex.slice.activation.domain.Service;
import com.candronex.slice.activation.domain.ServiceState;
import com.candronex.slice.activation.domain.port.ServiceActivationRepository;
import com.candronex.slice.inventory.application.RecordServiceStateUseCase;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Le travail transactionnel d'UC-06 : service et inventaire dans la même transaction locale. */
@Component
class ActivationTransitions {

    private final ServiceActivationRepository services;
    private final RecordServiceStateUseCase inventory;
    private final ApplicationEventPublisher events;

    ActivationTransitions(ServiceActivationRepository services, RecordServiceStateUseCase inventory,
                          ApplicationEventPublisher events) {
        this.services = services;
        this.inventory = inventory;
        this.events = events;
    }

    /** Enregistre un nouveau service PENDING ; l'appel au réseau suivra la validation. */
    @Transactional
    public void add(Service service) {
        services.add(service);
        inventory.record(service.serviceId(), service.droneId(), service.state());
        events.publishEvent(new ServiceAccepted(service.serviceId()));
    }

    /** @return faux si un autre traitement a déjà fait évoluer le service */
    @Transactional
    public boolean save(Service service, ServiceState expected) {
        if (!services.saveIfStillIn(service, expected)) {
            return false;
        }
        inventory.record(service.serviceId(), service.droneId(), service.state());
        if (service.state() == ServiceState.PENDING) {
            events.publishEvent(new ServiceAccepted(service.serviceId()));
        }
        return true;
    }
}
