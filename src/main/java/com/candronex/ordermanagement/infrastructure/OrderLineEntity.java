package com.candronex.ordermanagement.infrastructure;

import com.candronex.ordermanagement.domain.OrderLine;
import com.candronex.ordermanagement.domain.OrderLineStatus;
import com.candronex.ordermanagement.domain.OrderedService;
import com.candronex.ordermanagement.domain.ServiceCharacteristic;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Correspondance relationnelle d'une ligne de l'agrégat Order et de ses caractéristiques figées. */
@Entity
@Table(schema = "orders", name = "order_lines")
class OrderLineEntity {

    @Id
    @Column(name = "order_line_id", nullable = false, updatable = false)
    private UUID orderLineId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private OrderEntity order;

    /** Rang dans la commande : restitue l'ordre de la demande. */
    @Column(name = "position", nullable = false, updatable = false)
    private int position;

    @Column(name = "drone_id", nullable = false, updatable = false)
    private UUID droneId;

    @Column(name = "service_specification_id", nullable = false, updatable = false, length = 32)
    private String serviceSpecificationId;

    @Column(name = "service_specification_version", nullable = false, updatable = false)
    private int serviceSpecificationVersion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "orders", name = "order_line_characteristics",
            joinColumns = @JoinColumn(name = "order_line_id"))
    @OrderColumn(name = "position")
    private List<CharacteristicRow> characteristics = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private OrderLineStatus status;

    /** Requis par l'ORM. */
    protected OrderLineEntity() {
    }

    static OrderLineEntity from(OrderLine line, int position, OrderEntity order) {
        OrderLineEntity entity = new OrderLineEntity();
        entity.orderLineId = line.orderLineId();
        entity.order = order;
        entity.position = position;
        entity.droneId = line.droneId();
        entity.serviceSpecificationId = line.service().serviceSpecificationId();
        entity.serviceSpecificationVersion = line.service().serviceSpecificationVersion();
        entity.status = line.status();
        line.service().characteristics()
                .forEach(characteristic -> entity.characteristics.add(CharacteristicRow.from(characteristic)));
        return entity;
    }

    OrderLine toDomain() {
        OrderedService service = new OrderedService(
                serviceSpecificationId,
                serviceSpecificationVersion,
                characteristics.stream().map(CharacteristicRow::toDomain).toList());
        return OrderLine.restore(orderLineId, droneId, service, status);
    }

    /** Une ligne de orders.order_line_characteristics. */
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
