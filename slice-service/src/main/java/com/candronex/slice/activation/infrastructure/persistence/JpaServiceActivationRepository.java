package com.candronex.slice.activation.infrastructure.persistence;

import com.candronex.slice.activation.domain.Service;
import com.candronex.slice.activation.domain.ServiceState;
import com.candronex.slice.activation.domain.port.ServiceActivationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptateur sortant : réalise le port ServiceActivationRepository. */
@Repository
class JpaServiceActivationRepository implements ServiceActivationRepository {

    private final ServiceEntityStore services;
    private final AttemptEntityStore attempts;

    JpaServiceActivationRepository(ServiceEntityStore services, AttemptEntityStore attempts) {
        this.services = services;
        this.attempts = attempts;
    }

    @Override
    public Optional<Service> findById(UUID serviceId) {
        return services.findById(serviceId).map(this::toDomain);
    }

    @Override
    public Optional<Service> findByOrderLineId(UUID orderLineId) {
        return services.findByOrderLineId(orderLineId).map(this::toDomain);
    }

    @Override
    public List<Service> findAllByState(ServiceState state) {
        return services.findAllByState(state).stream().map(this::toDomain).toList();
    }

    @Override
    public void add(Service service) {
        // Flush immédiat : un doublon d'orderLineId est refusé ici, dans la transaction.
        services.saveAndFlush(ServiceEntity.from(service));
        saveAttempts(service);
    }

    @Override
    public boolean saveIfStillIn(Service service, ServiceState expected) {
        if (services.updateState(service.serviceId(), expected, service.state()) == 0) {
            return false;
        }
        saveAttempts(service);
        return true;
    }

    private void saveAttempts(Service service) {
        attempts.saveAllAndFlush(service.attempts().stream()
                .map(attempt -> AttemptEntity.from(service.serviceId(), attempt))
                .toList());
    }

    private Service toDomain(ServiceEntity entity) {
        return entity.toDomain(attempts.findAllByServiceIdOrderByAcceptedAt(entity.serviceId()).stream()
                .map(AttemptEntity::toDomain)
                .toList());
    }
}
