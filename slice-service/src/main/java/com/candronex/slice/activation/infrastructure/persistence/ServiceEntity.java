package com.candronex.slice.activation.infrastructure.persistence;

import com.candronex.slice.activation.domain.ActivationAttempt;
import com.candronex.slice.activation.domain.Service;
import com.candronex.slice.activation.domain.ServiceCharacteristic;
import com.candronex.slice.activation.domain.ServiceState;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Correspondance relationnelle de l'agrégat Service ; ses tentatives sont dans AttemptEntity. */
@Entity
@Table(schema = "activation", name = "services")
class ServiceEntity {

    @Id
    @Column(name = "service_id", nullable = false, updatable = false)
    private UUID serviceId;

    @Column(name = "order_line_id", nullable = false, updatable = false)
    private UUID orderLineId;

    @Column(name = "drone_id", nullable = false, updatable = false)
    private UUID droneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private ServiceState state;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "activation", name = "service_characteristics",
            joinColumns = @JoinColumn(name = "service_id"))
    @OrderColumn(name = "position")
    private List<CharacteristicRow> characteristics = new ArrayList<>();

    /** Requis par l'ORM. */
    protected ServiceEntity() {
    }

    static ServiceEntity from(Service service) {
        ServiceEntity entity = new ServiceEntity();
        entity.serviceId = service.serviceId();
        entity.orderLineId = service.orderLineId();
        entity.droneId = service.droneId();
        entity.state = service.state();
        service.characteristics()
                .forEach(characteristic -> entity.characteristics.add(CharacteristicRow.from(characteristic)));
        return entity;
    }

    Service toDomain(List<ActivationAttempt> attempts) {
        return Service.restore(serviceId, orderLineId, droneId,
                characteristics.stream().map(CharacteristicRow::toDomain).toList(), attempts, state);
    }

    UUID serviceId() {
        return serviceId;
    }

    /** Une ligne de activation.service_characteristics. */
    @Embeddable
    static class CharacteristicRow {

        @Column(name = "name", nullable = false, length = 32)
        private String name;

        @Column(name = "value", nullable = false, length = 64)
        private String value;

        /** Requis par l'ORM. */
        protected CharacteristicRow() {
        }

        static CharacteristicRow from(ServiceCharacteristic characteristic) {
            CharacteristicRow row = new CharacteristicRow();
            row.name = characteristic.name();
            row.value = characteristic.value();
            return row;
        }

        ServiceCharacteristic toDomain() {
            return new ServiceCharacteristic(name, value);
        }
    }
}
