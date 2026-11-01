package com.candronex.order.catalog.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Racine d'agrégat du contexte ServiceCatalog (TMF633) : un service que le client peut commander,
 * et les caractéristiques qui le décrivent.
 */
public final class ServiceSpecification {

    private final ServiceSpecificationId id;
    private final String name;
    private final List<CharacteristicSpecification> characteristics;

    private ServiceSpecification(ServiceSpecificationId id, String name,
                                 List<CharacteristicSpecification> characteristics) {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant de la spécification est obligatoire.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de la spécification est obligatoire.");
        }
        if (characteristics == null || characteristics.isEmpty()) {
            throw new IllegalArgumentException("Une spécification décrit au moins une caractéristique.");
        }
        Set<String> names = new HashSet<>();
        for (CharacteristicSpecification characteristic : characteristics) {
            if (!names.add(characteristic.name())) {
                throw new IllegalArgumentException(
                        "Caractéristique en double : " + characteristic.name() + ".");
            }
        }
        this.id = id;
        this.name = name;
        this.characteristics = List.copyOf(characteristics);
    }

    /** Reconstitue une spécification publiée ; le catalogue est alimenté par migration. */
    public static ServiceSpecification restore(ServiceSpecificationId id, String name,
                                               List<CharacteristicSpecification> characteristics) {
        return new ServiceSpecification(id, name, characteristics);
    }

    public ServiceSpecificationId id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** Dans l'ordre de publication ; liste non modifiable. */
    public List<CharacteristicSpecification> characteristics() {
        return characteristics;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ServiceSpecification that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
