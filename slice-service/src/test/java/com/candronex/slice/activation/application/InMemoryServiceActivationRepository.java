package com.candronex.slice.activation.application;

import com.candronex.slice.activation.domain.ActivationAttempt;
import com.candronex.slice.activation.domain.Service;
import com.candronex.slice.activation.domain.ServiceState;
import com.candronex.slice.activation.domain.port.ServiceActivationRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Réalisation en mémoire du port ; garde une copie, comme une base, et non l'objet modifié. */
class InMemoryServiceActivationRepository implements ServiceActivationRepository {

    private final Map<UUID, Service> byId = new LinkedHashMap<>();

    @Override
    public Optional<Service> findById(UUID serviceId) {
        return Optional.ofNullable(byId.get(serviceId)).map(InMemoryServiceActivationRepository::copy);
    }

    @Override
    public Optional<Service> findByOrderLineId(UUID orderLineId) {
        return byId.values().stream()
                .filter(service -> service.orderLineId().equals(orderLineId))
                .findFirst()
                .map(InMemoryServiceActivationRepository::copy);
    }

    @Override
    public List<Service> findAllByState(ServiceState state) {
        return byId.values().stream().filter(service -> service.state() == state)
                .map(InMemoryServiceActivationRepository::copy).toList();
    }

    @Override
    public void add(Service service) {
        byId.put(service.serviceId(), copy(service));
    }

    @Override
    public boolean saveIfStillIn(Service service, ServiceState expected) {
        if (byId.get(service.serviceId()).state() != expected) {
            return false;
        }
        byId.put(service.serviceId(), copy(service));
        return true;
    }

    int count() {
        return byId.size();
    }

    private static Service copy(Service service) {
        return Service.restore(service.serviceId(), service.orderLineId(), service.droneId(),
                service.characteristics(),
                service.attempts().stream()
                        .map(attempt -> ActivationAttempt.restore(attempt.attemptId(),
                                attempt.acceptedAt(), attempt.activatedAt(), attempt.result()))
                        .toList(),
                service.state());
    }
}
