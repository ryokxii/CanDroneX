package com.candronex.servicecatalog.infrastructure;

import com.candronex.servicecatalog.domain.CharacteristicSpecification;
import com.candronex.servicecatalog.domain.CharacteristicValueType;
import com.candronex.servicecatalog.domain.ServiceSpecification;
import com.candronex.servicecatalog.domain.ServiceSpecificationId;
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
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.List;

/**
 * Correspondance relationnelle de l'agrégat ServiceSpecification. En lecture seule : le catalogue
 * est alimenté par migration.
 */
@Entity
@Immutable
@Table(schema = "catalog", name = "service_specifications")
class ServiceSpecificationEntity {

    @Id
    @Column(name = "service_specification_id", nullable = false, updatable = false, length = 32)
    private String serviceSpecificationId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "version", nullable = false)
    private int version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "catalog", name = "characteristic_specifications",
            joinColumns = @JoinColumn(name = "service_specification_id"))
    @OrderColumn(name = "position")
    private List<CharacteristicRow> characteristics = new ArrayList<>();

    /** Requis par l'ORM. */
    protected ServiceSpecificationEntity() {
    }

    ServiceSpecification toDomain() {
        return ServiceSpecification.restore(
                ServiceSpecificationId.of(serviceSpecificationId),
                name,
                version,
                characteristics.stream().map(CharacteristicRow::toDomain).toList());
    }

    /** Une ligne de catalog.characteristic_specifications. */
    @Embeddable
    static class CharacteristicRow {

        @Column(name = "name", nullable = false, length = 32)
        private String name;

        @Enumerated(EnumType.STRING)
        @Column(name = "value_type", nullable = false, length = 16)
        private CharacteristicValueType valueType;

        @Column(name = "default_value", nullable = false, length = 64)
        private String defaultValue;

        /** Requis par l'ORM. */
        protected CharacteristicRow() {
        }

        CharacteristicSpecification toDomain() {
            return new CharacteristicSpecification(name, valueType, defaultValue);
        }
    }
}
